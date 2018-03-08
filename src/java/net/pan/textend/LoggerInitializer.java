package net.pan.textend;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

import javafx.application.Application;

/**
 * Parses program arguments to initialize {@code Logger}s.
 * <p>
 * The following command-line arguments are understood:
 * <dl>
 *
 * <dt>{@code --conlevel} <var>level</var>
 * <dt>{@code --conlevel}=<var>level</var>
 * <dd>Sets the logging level of all {@code ConsoleHandlers}.
 * <var>level</var> must be a valid constant in the
 * {@code java.util.logging.Level} class.
 *
 * <dt>{@code --loglevel} <var>level-spec</var>
 * <dt>{@code --loglevel}=<var>level-spec</var>
 * <dt>{@code --ll} <var>level-spec</var>
 * <dt>{@code --ll}=<var>level-spec</var>
 * <dd>Sets one or more {@code Logger}s to a specific level.
 * <var>level-spec</var> takes the form:
 * <p><var>logger-name</var>{@code :}<var>level</var>
 * [{@code ,}<var>logger-name</var>{@code :}<var>level</var> &#x2026;]
 * <p><var>level</var> must be a valid constant in the
 * {@code java.util.logging.Level} class.
 * <p>Examples:
 * <p>{@code --loglevel=com.example.viewer.PrefsWindow:FINE}
 * <p>{@code --loglevel=com.example.server:CONFIG,com.example.server.SocketFactory:ALL}
 *
 * </dl>
 */
class LoggerInitializer
{
    private static final Logger rootLogger = Logger.getLogger("");

    private static final Map<String, Logger> configuredLoggers =
        new HashMap<String, Logger>();

    /** Prevents instantiation. */
    private LoggerInitializer()
    {
        // Deliberately empty.
    }

    static void setConsoleLevel(Level level)
    {
        for (Handler handler : rootLogger.getHandlers())
        {
            if (handler instanceof ConsoleHandler)
            {
                handler.setLevel(level);
            }
        }
    }

    private static void setLogLevel(String spec)
    {
        int colon = spec.lastIndexOf(':');
        String logname = spec.substring(0, Math.max(0, colon));
        Level level = Level.parse(spec.substring(colon + 1));
        Logger logger = Logger.getLogger(logname);
        logger.setLevel(level);
        configuredLoggers.put(logname, logger); // prevent GC of Logger
    }

    private static void setLogLevels(String spec)
    {
        for (String s : spec.split(","))
        {
            setLogLevel(s.trim());
        }
    }

    private static String parseArg(String arg,
                                   Iterator<String> i)
    {
        int equals = arg.indexOf('=');
        if (equals > 0)
        {
            return arg.substring(equals + 1);
        }

        if (i.hasNext())
        {
            String value = i.next();
            i.remove();
            return value;
        }

        return "";
    }

    static String[] parseLogLevelOptions(String[] mainArgs)
    {
        List<String> argList = new ArrayList<String>(Arrays.asList(mainArgs));
        return argList.toArray(new String[0]);
    }

    static List<String> parseLogLevelOptions(List<String> args)
    {
        args = new ArrayList<>(args);

        Level consoleLevel = Level.ALL;

        Iterator<String> i = args.iterator();
        while (i.hasNext())
        {
            String arg = i.next();
            if (arg.matches("--?(?i:conlevel)(=.*)?"))
            {
                i.remove();
                consoleLevel = Level.parse(parseArg(arg, i));
            }
            if (arg.matches("--?(?i:loglevel|ll)(=.*)?"))
            {
                i.remove();
                setLogLevels(parseArg(arg, i));
            }
        }

        setConsoleLevel(consoleLevel);

        return args;
    }

    static List<String> parseLogLevelOptions(
        Application.Parameters params)
    {
        return parseLogLevelOptions(new ArrayList<String>(params.getRaw()));
    }
}
