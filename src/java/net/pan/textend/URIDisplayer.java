package net.pan.textend;

import java.io.IOException;

import java.util.Objects;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.application.Application;

/**
 * Opens a URI in the host system's default application, which for web URLs
 * is the default web browser.
 */
class URIDisplayer
{
    private static final Logger logger =
        Logger.getLogger(URIDisplayer.class.getName());

    private final Application application;

    URIDisplayer(Application application)
    {
        this.application = Objects.requireNonNull(application,
            "Application cannot be null");
    }

    void openURI(String uri)
    {
        // Workaround for 
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8087385
        // See also: https://github.com/vinumeris/lighthouse/issues/185
        if (System.getProperty("os.name").contains("Linux"))
        {
            try
            {
                new ProcessBuilder("xdg-open", uri).inheritIO().start();
                return;
            }
            catch (IOException e)
            {
                logger.log(Level.INFO, "Couldn't run xdg-open", e);
            }

            try
            {
                new ProcessBuilder("gnome-open", uri).inheritIO().start();
                return;
            }
            catch (IOException e)
            {
                logger.log(Level.INFO, "Couldn't run gnome-open", e);
            }

            logger.config("Falling back on HostServices.showDocument");
        }

        application.getHostServices().showDocument(uri);
    }
}
