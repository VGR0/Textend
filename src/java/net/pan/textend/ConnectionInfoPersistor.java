package net.pan.textend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.PosixFilePermissions;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.EnumSet;
import java.util.Objects;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.beans.DefaultPersistenceDelegate;
//import java.beans.Expression;
import java.beans.ExceptionListener;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;

class ConnectionInfoPersistor
extends Service<ObservableList<ConnectionInfo>>
{
    private static final Logger logger =
        Logger.getLogger(ConnectionInfoPersistor.class.getName());

    enum Operation
    {
        LOAD,
        SAVE
    }

    private Operation operation = Operation.LOAD;

    private ObservableList<ConnectionInfo> lastGoodValue;

    Operation getOperation()
    {
        return operation;
    }

    void setOperation(Operation op)
    {
        this.operation = Objects.requireNonNull(op, "Operation cannot be null");
    }

    @Override
    protected Task<ObservableList<ConnectionInfo>> createTask()
    {
        Operation op = getOperation();

        logger.fine(() -> "Operation=" + op);
        switch (op)
        {
            case LOAD:
                return new Loader();
            case SAVE:
                return new Saver(lastGoodValue);
            default:
                break;
        }
        throw new RuntimeException("Illegal Operation value: " + op);
    }

    @Override
    protected void succeeded()
    {
        this.lastGoodValue = getValue();
    }

    private static Path getSaveFile()
    {
        return SystemDirectories.configDir().resolve("connections.xml");
    }

    private static class Loader
    extends Task<ObservableList<ConnectionInfo>>
    {
        @Override
        protected ObservableList<ConnectionInfo> call()
        throws IOException
        {
            Path file = getSaveFile();
            if (!Files.exists(file))
            {
                return FXCollections.observableArrayList();
            }

            logger.config(() -> "Loading from " + file);

            Exception[] exception = { null };
            try (XMLDecoder decoder =
                new XMLDecoder(
                    new BufferedInputStream(
                        Files.newInputStream(file)),
                    this, e -> exception[0] = e))
            {
                ConnectionInfo[] info = (ConnectionInfo[]) decoder.readObject();

                if (isCancelled())
                {
                    if (exception[0] != null)
                    {
                        logger.log(Level.FINE, "Canceled", exception[0]);
                    }
                    return null;
                }

                if (exception[0] != null)
                {
                    throw new IOException(exception[0]);
                }

                return FXCollections.observableArrayList(info);
            }
        }

        @Override
        protected void failed()
        {
            updateValue(FXCollections.observableArrayList());
        }
    }

    private static class Saver
    extends Task<ObservableList<ConnectionInfo>>
    {
        // The file will contain passwords, so we want to give it
        // secure permissions.

        private static final String SAVE_FILE_PERMISSIONS = "rw-------";

        private static final Set<AclEntryPermission> SAVE_FILE_ACL_PERMISSIONS =
            Collections.unmodifiableSet(EnumSet.of(
                AclEntryPermission.READ_DATA,
                AclEntryPermission.WRITE_DATA,
                AclEntryPermission.APPEND_DATA,
                AclEntryPermission.DELETE,
                AclEntryPermission.READ_ACL,
                AclEntryPermission.WRITE_ACL,
                AclEntryPermission.READ_ATTRIBUTES,
                AclEntryPermission.WRITE_ATTRIBUTES,
                AclEntryPermission.READ_NAMED_ATTRS,
                AclEntryPermission.WRITE_NAMED_ATTRS,
                AclEntryPermission.WRITE_OWNER,
                AclEntryPermission.SYNCHRONIZE));

        private final ObservableList<ConnectionInfo> info;

        Saver(ObservableList<ConnectionInfo> info)
        {
            this.info = Objects.requireNonNull(info,
                "ConnectionInfo list cannot be null");
        }

        @Override
        protected ObservableList<ConnectionInfo> call()
        throws IOException
        {
            logger.entering(Saver.class.getName(), "call");

            Path file = getSaveFile();
            logger.config(() -> "Saving to " + file);

            Files.createDirectories(file.getParent());

            Path backup;
            if (Files.exists(file))
            {
                backup = Files.createTempFile(null, null);
                Files.move(file, backup, REPLACE_EXISTING);
                logger.fine(() -> "Moved old save file from \"" + file + "\""
                    + " to \"" + backup + "\"");
            }
            else
            {
                backup = null;
            }

            Exception[] exception = { null };
            try (XMLEncoder encoder =
                new XMLEncoder(
                    new BufferedOutputStream(
                        Files.newOutputStream(file))))
            {
                encoder.setPersistenceDelegate(Color.class,
                    new DefaultPersistenceDelegate(
                        new String[] { "red", "green", "blue", "opacity" }));
                // TODO: Should consider writing a delegate that calls
                // static factory method Color.color.
                /*
                @Override
                protected Expression instantiate(Object colorObj,
                                                 Encoder out)
                {
                    Color color = (Color) colorObj;
                    return new Expression(color, Color.class, "color",
                        new Object[] {
                            color.getRed(), color.getGreen(), color.getBlue(), 
                            color.getOpacity()
                        });
                }
                 */

                encoder.setExceptionListener(e -> exception[0] = e);

                encoder.writeObject(info.toArray(new ConnectionInfo[0]));
            }

            if (isCancelled())
            {
                if (exception[0] != null)
                {
                    logger.log(Level.FINE, "Canceled", exception[0]);
                }
                else
                {
                    logger.fine("Canceled");
                }

                logger.info(() -> "Save failed"
                    + ";  restoring old file by copying \"" + backup + "\""
                    + " to \"" + file + "\"");
                Files.move(backup, file, REPLACE_EXISTING);
            }
            else
            {
                if (exception[0] != null)
                {
                    logger.info(() -> "Save failed"
                        + ";  restoring old file by copying \"" + backup + "\""
                        + " to \"" + file + "\"");
                    try
                    {
                        Files.move(backup, file, REPLACE_EXISTING);
                    }
                    catch (IOException e)
                    {
                        logger.log(Level.WARNING, "Cannot restore backup", e);
                    }

                    throw new IOException(exception[0]);
                }

                FileStore store = Files.getFileStore(file);
                if (store.supportsFileAttributeView("posix"))
                {
                    logger.fine(() -> "Setting permissions of \"" + file + "\""
                        + " to " + SAVE_FILE_PERMISSIONS);
                    Files.setPosixFilePermissions(file,
                        PosixFilePermissions.fromString(SAVE_FILE_PERMISSIONS));
                }
                else if (store.supportsFileAttributeView("acl"))
                {
                    AclEntry.Builder builder = AclEntry.newBuilder();
                    builder.setType(AclEntryType.ALLOW);
                    builder.setPrincipal(Files.getOwner(file.getParent()));
                    builder.setPermissions(SAVE_FILE_ACL_PERMISSIONS);
                    AclEntry entry = builder.build();
                    Files.setAttribute(file, "acl:acl",
                        Collections.singletonList(entry));
                }

                if (backup != null)
                {
                    logger.fine(() -> "Deleting backup \"" + backup + "\"");
                    Files.delete(backup);
                }
            }

            logger.exiting(Saver.class.getName(), "call");

            return info;
        }
    }

    @Override
    protected void failed()
    {
        Throwable error = getException();
        if (error != null)
        {
            logger.log(Level.WARNING,
                "Operation " + operation + " failed", error);
        }
    }
}
