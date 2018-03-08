package net.pan.textend;

import java.security.GeneralSecurityException;

import java.io.Closeable;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.SocketChannel;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.util.Objects;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.function.Consumer;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Implements telnet protocol over a socket.  The entire session is managed
 * in the {@link #call()} method.
 */
class TelnetConnection
implements Callable<Void>
{
    private static final Logger logger =
        Logger.getLogger(TelnetConnection.class.getName());

    // TODO: configurable (per world?)
    private static final int PROMPT_TIMEOUT = Math.max(250, Integer.getInteger(
        TelnetConnection.class.getPackage().getName() + ".promptThreshold",
        2000));

    /** Interpret As Command. */
    private static final int IAC = 255;

    private static class Command
    {
        /** End of Record. */
        static final int EOR = 239;

        /** Sub-negotiation End. */
        static final int SE = 240;

        static final int NOP = 241;

        /** Data Mark. */
        static final int DM = 242;

        static final int BRK = 243;

        /** Interrupt Process. */
        static final int IP = 244;

        /** Abort Output. */
        static final int AO = 245;

        /** Are You There. */
        static final int AYT = 246;

        /** Erase Character. */
        static final int EC = 247;

        /** Erase Line. */
        static final int EL = 248;

        /** Go Ahead. */
        static final int GA = 249;

        /** Sub-negotiation Begin. */
        static final int SB = 250;

        static final int WILL = 251;

        static final int WONT = 252;

        static final int DO = 253;

        static final int DONT = 254;

        static final TelnetByteNames names = new TelnetByteNames(Command.class);

        static String getName(int cmd)
        {
            return names.getName(cmd);
        }
    }

    private static class Option
    {
        static final int BINARY = 0;
        static final int ECHO = 1;
        static final int RECONNECT = 2;
        static final int SUPPRESS_GOAHEAD = 3;
        static final int STATUS = 5;
        static final int LINE_WIDTH = 8;
        static final int END_OF_RECORD = 25;
        /** Negotiation About Window Size. */
        static final int NAWS = 31;
        static final int LINEMODE = 34;

        static final TelnetByteNames names = new TelnetByteNames(Option.class);

        static String getName(int opt)
        {
            return names.getName(opt);
        }
    }

    /**
     * @see <a href="http://tools.ietf.org/html/rfc1184">RFC 1184 - Telnet Linemode Option</a>
     */
    private interface LineMode
    {
        int MODE = 1;
        int MODE_EDIT       = 1 << 0;
        int MODE_TRAPSIG    = 1 << 1;
        int MODE_ACK        = 1 << 2;
        int MODE_SOFT_TAB   = 1 << 3;
        int MODE_LIT_ECHO   = 1 << 4;

        int FORWARDMASK = 2;
        /** Set Local Characters. */
        int SLC = 3;
        int EOF = 236;
        int SUSP = 237;
        int ABORT = 238;
    }

    private enum IACResult
    {
        NORMAL,
        NEED_FURTHER_READ,
        EOF
    }

    private final ExecutorService socketReadExecutor;

    private final Object writeMonitor = new Object();

    private final Closeable connection;

    private final SocketAddress address;

    private final int socketReceiveBufferSize;

    private final Charset charset;

    private final Consumer<String> readListener;

    private final ReadableByteChannel input;

    private final WritableByteChannel output;

    private boolean readyToProcessRead;

    /** Set, but not currently used. */
    private boolean echo = true;

    private boolean endOfRecord = false;

    static TelnetConnection create(String host,
                                   int port,
                                   Charset charset,
                                   Consumer<String> readListener)
    throws IOException
    {
        return create(host, port, charset,
            false, SSLProtocol.defaultProtocol(), false, readListener);
    }

    static TelnetConnection create(String host,
                                   int port,
                                   Charset charset,
                                   boolean ssl,
                                   SSLProtocol sslProtocol,
                                   boolean requireValidCertificates,
                                   Consumer<String> readListener)
    throws IOException
    {
        if (ssl)
        {
            Objects.requireNonNull(sslProtocol,
                "Charset cannot be null if ssl is true");
        }

        if (ssl)
        {
            Socket socket = new Socket(host, port);
            socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            socket.setOption(StandardSocketOptions.TCP_NODELAY, true);

            TrustManager[] trustManagers = null;
            if (!requireValidCertificates)
            {
                trustManagers = new TrustManager[] { new DummyTrustManager() };
            }

            SSLContext context;
            try
            {
                String protocol = sslProtocol.standardName;
                context = SSLContext.getInstance(protocol);
                context.init(null, trustManagers, null);
            }
            catch (GeneralSecurityException e)
            {
                throw new IOException(e);
            }

            socket = context.getSocketFactory().createSocket(socket,
                host, port, true);

            return new TelnetConnection(socket, charset, readListener);
        }
        else
        {
            SocketChannel channel = SocketChannel.open();
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            channel.connect(new InetSocketAddress(host, port));

            return new TelnetConnection(channel, charset, readListener);
        }
    }

    private TelnetConnection(SocketChannel channel,
                             Charset charset,
                             Consumer<String> readListener)
    throws IOException
    {
        this.connection = channel;
        this.address = channel.getRemoteAddress();
        this.readListener = Objects.requireNonNull(readListener,
            "Read listener cannot be null");
        this.charset = Objects.requireNonNull(charset,
            "Charset cannot be null");
        socketReadExecutor = Executors.newSingleThreadExecutor();

        socketReceiveBufferSize =
            channel.getOption(StandardSocketOptions.SO_RCVBUF);

        input = channel;
        output = channel;
    }

    private TelnetConnection(Socket socket,
                             Charset charset,
                             Consumer<String> readListener)
    throws IOException
    {
        this.connection = socket;
        this.address = socket.getRemoteSocketAddress();
        this.readListener = Objects.requireNonNull(readListener,
            "Read listener cannot be null");
        this.charset = Objects.requireNonNull(charset,
            "Charset cannot be null");
        socketReadExecutor = Executors.newSingleThreadExecutor();

        socketReceiveBufferSize =
            socket.getOption(StandardSocketOptions.SO_RCVBUF);

        input = Channels.newChannel(socket.getInputStream());
        output = Channels.newChannel(socket.getOutputStream());
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "[" + address + "]";
    }

    /**
     * For debugging.
     */
    private static String toText(ByteBuffer buffer)
    {
        return Chat.escape(String.valueOf(
            StandardCharsets.ISO_8859_1.decode(buffer)));
    }

    private static class ReadState
    {
        final ByteBuffer buffer;
        Future<ByteBuffer> lastRead;
        /**
         * Start position in buffer of text which hasn't been conveyed to UI.
         * -1 means "no text in the buffer to send yet."
         */
        int textStart = -1;

        ReadState(ByteBuffer buffer)
        {
            this.buffer = Objects.requireNonNull(buffer,
                "Buffer cannot be null");
        }

        @Override
        public String toString()
        {
            return "ReadState["
                + "textStart=" + textStart + ", lastRead=" + lastRead + "]";
        }
    }

    /**
     * Reads from this connection's socket until it closes.
     *
     * @return always {@code null}
     */
    @Override
    public Void call()
    throws IOException,
           ExecutionException,
           InterruptedException
            
    {
        ByteBuffer buffer = ByteBuffer.allocate(socketReceiveBufferSize * 2);
        logger.config(() -> String.format(
            "Created buffer with size %,d", buffer.capacity()));

        ReadState state = new ReadState(buffer);

        Callable<ByteBuffer> socketReader =
            new Callable<ByteBuffer>()
            {
                @Override
                public ByteBuffer call()
                throws IOException
                {
                    ByteBuffer buffer =
                        ByteBuffer.allocate(socketReceiveBufferSize);
                    int bytesRead = input.read(buffer);
                    if (bytesRead < 0)
                    {
                        return null;
                    }
                    buffer.flip();
                    return buffer;
                }
            };


        boolean timedOut = false;

        readLoop:
        while (true)
        {
            if (state.lastRead == null)
            {
                state.lastRead = socketReadExecutor.submit(socketReader);
            }

            ByteBuffer newBytes;
            if (!timedOut)
            {
                try
                {
                    newBytes = state.lastRead.get(PROMPT_TIMEOUT, MILLISECONDS);
                    timedOut = false;
                }
                catch (TimeoutException e)
                {
                    logger.log(Level.FINER,
                        "Read timed out;  assuming prompt.", e);
                    logger.finest(() -> "textStart=" + state.textStart +
                        ", current prompt in buffer is \"" +
                        Chat.escape(toText(buffer.duplicate().flip())) + "\"");

                    newBytes = ByteBuffer.allocate(0);
                    timedOut = true;
                }
            }
            else
            {
                newBytes = state.lastRead.get();
                timedOut = false;   // TODO: remove this if not needed
            }

            if (newBytes == null)
            {
                break;
            }

            if (!timedOut)
            {
                buffer.put(newBytes);
                state.lastRead = null;
            }

            buffer.flip();
            logger.finest(() -> "Read into buffer: \"" +
                toText(buffer.duplicate()) + "\"");

            int b;
            bytesProcessingLoop:
            while (buffer.hasRemaining())
            {
                b = buffer.get() & 0xff;
                if (b == IAC)
                {
                    IACResult result = handleIAC(state);
                    switch (result)
                    {
                        case NEED_FURTHER_READ:
                            break bytesProcessingLoop;
                        case EOF:
                            break readLoop;
                        default:
                            break;
                    }
                }
                else
                {
                    // Got a readable character.

                    if (state.textStart < 0)
                    {
                        state.textStart = buffer.position() - 1;
                    }

                    if (timedOut)
                    {
                        logger.finest(
                            () -> "Timed out, remaining=" + buffer.remaining() +
                                ", text=\"" + toText(buffer.duplicate()) +
                                "\"");
                    }

                    if (b == '\n' || (timedOut && !buffer.hasRemaining()))
                    {
                        notifyReadListener(state);
                        state.textStart = -1;
                    }
                }
            }

            // Remove everything before textStart.
            if (state.textStart < 0)
            {
                buffer.clear();
            }
            else
            {
                buffer.position(state.textStart);
                buffer.compact();
            }
            state.textStart = 0;
        }

        return null;
    }

    private IACResult handleIAC(ReadState state)
    throws IOException,
           ExecutionException,
           InterruptedException
    {
        if (!state.buffer.hasRemaining())
        {
            // Save IAC to be read again in next iteration.
            state.buffer.position(state.buffer.position() - 1);
            return IACResult.NEED_FURTHER_READ;
        }
        int cmd = state.buffer.get() & 0xff;

        logger.fine(() ->
            "Received telnet command " + Command.getName(cmd));

        if (cmd == IAC)
        {
            // literal 0xff;  delete duplicated byte
            state.buffer.position(state.buffer.position() - 1);
            deleteByte(state.buffer);

            // Byte is not a telnet directive, so treat it as text.
            if (state.textStart < 0)
            {
                state.textStart = state.buffer.position() - 1;
            }

            return IACResult.NEED_FURTHER_READ;
        }

        // Convey any received text prior to this telnet command
        // to the UI.
        // TODO: This isn't mandated by telnet protocol;  not sure
        // if we should do it.
        int currentPos = state.buffer.position();
        state.buffer.position(state.buffer.position() - 2);
        notifyReadListener(state);
        state.textStart = -1;
        state.buffer.position(currentPos);

        if (cmd == Command.WILL || cmd == Command.WONT ||
            cmd == Command.DO || cmd == Command.DONT)
        {
            if (!state.buffer.hasRemaining())
            {
                // Save IAC & cmd to be read again
                // in next iteration.
                state.buffer.position(state.buffer.position() - 2);
                return IACResult.NEED_FURTHER_READ;
            }
            int opt = state.buffer.get() & 0xff;

            logger.fine(() -> "Received telnet option " +
                Option.getName(opt));

            switch (opt)
            {
                case Option.ECHO:
                    switch (cmd)
                    {
                        case Command.DO:
                            echo = true;
                            break;
                        case Command.DONT:
                            echo = false;
                            break;
                    }
                    acknowledge(cmd, opt);
                    break;
                case Option.END_OF_RECORD:
                    switch (cmd)
                    {
                        case Command.DO:
                            endOfRecord = true;
                            break;
                        case Command.DONT:
                            endOfRecord = false;
                            break;
                    }
                    acknowledge(cmd, opt);
                    break;
                case Option.LINEMODE:
                    // TODO
                    refuse(cmd, opt);
                    break;
                default:
                    refuse(cmd, opt);
                    break;
            }
        }
        else if (cmd == Command.SB)
        {
            logger.fine("Got SB, consuming bytes until SE found");
            int b;
            do
            {
                if (!state.buffer.hasRemaining())
                {
                    state.buffer.clear();
                    if (state.lastRead != null)
                    {
                        ByteBuffer newBytes = state.lastRead.get();
                        if (newBytes == null)
                        {
                            return IACResult.EOF;
                        }
                        state.buffer.put(newBytes);
                    }
                    else
                    {
                        if (input.read(state.buffer) < 0)
                        {
                            return IACResult.EOF;
                        }
                    }

                    state.buffer.flip();
                }
                b = state.buffer.get() & 0xff;
            }
            while (b != Command.SE);
        }
        else if (cmd == Command.EC)
        {
            // TODO
        }
        else if (cmd == Command.EL)
        {
            // TODO
        }
        else if (cmd == Command.GA)
        {
            notifyReadListener(state);
            state.textStart = -1;
        }
        else if (cmd == Command.EOR)
        {
            notifyReadListener(state);
            state.textStart = -1;
        }

        return IACResult.NORMAL;
    }

    private void deleteByte(ByteBuffer buffer)
    {
        ByteBuffer remainder = buffer.slice();
        remainder.get();
        remainder.compact();
        buffer.limit(buffer.limit() - 1);
    }

    private void notifyReadListener(ReadState state)
    {
        notifyReadListener(state.buffer, state.textStart);
    }

    private void notifyReadListener(ByteBuffer buffer,
                                    int textStart)
    {
        logger.finest(() -> "textStart=" + textStart);
        if (textStart < 0)
        {
            return;
        }

        ByteBuffer textBuffer = buffer.duplicate();
        textBuffer.flip().position(textStart);
        String text = charset.decode(textBuffer).toString();

        logger.finest(() -> "Received \"" + Chat.escape(text) + "\"");

        readListener.accept(text);
    }

    private void acknowledge(int command,
                             int opt)
    throws IOException
    {
        switch (command)
        {
            case Command.WILL:
                sendCommand(Command.DO, opt);
                break;
            case Command.WONT:
                sendCommand(Command.DONT, opt);
                break;
            case Command.DO:
                sendCommand(Command.WILL, opt);
                break;
            case Command.DONT:
                sendCommand(Command.WONT, opt);
                break;
        }
    }

    private void refuse(int command,
                        int opt)
    throws IOException
    {
        switch (command)
        {
            case Command.WILL:
                sendCommand(Command.DONT, opt);
                break;
            case Command.WONT:
                sendCommand(Command.DO, opt);
                break;
            case Command.DO:
                sendCommand(Command.WONT, opt);
                break;
            case Command.DONT:
                sendCommand(Command.WILL, opt);
                break;
        }
    }

    private void sendCommand(int command,
                             int... bytes)
    throws IOException
    {
        logger.fine(() -> "Sending IAC " + Command.getName(command) +
            (bytes.length > 0 ? (" " + Option.getName(bytes[0])) : "") +
            (bytes.length > 1 ? " ..." : ""));

        ByteBuffer allBytes = ByteBuffer.allocate(bytes.length + 2);
        allBytes.put((byte) IAC);
        allBytes.put((byte) command);
        for (int b : bytes)
        {
            allBytes.put((byte) b);
        }
        allBytes.flip();

        send(allBytes);
    }

    void write(String s)
    throws IOException
    {
        logger.finest(() -> "Sending \"" + Chat.escape(s) + "\"");
        synchronized (writeMonitor)
        {
            send(charset.encode(s));
            if (endOfRecord)
            {
                sendCommand(Command.EOR);
            }
            //channel.flush();
            //sendCommand(Command.GA);
        }
    }

    private void send(ByteBuffer bytes)
    throws IOException
    {
        logger.finest(() -> String.format(
            "Sending %,d bytes", bytes.remaining()));

        synchronized (writeMonitor)
        {
            while (bytes.hasRemaining())
            {
                output.write(bytes);
            }
            //output.flush();
        }

        logger.finest("Bytes sent.");
    }

    void close()
    throws IOException
    {
        connection.close();
    }
}
