/**
 * Application which allows connection to telnet-based talkers.
 *
 * @author VGR
 */
module net.pan.textend {
    requires java.logging;
    requires java.xml;
    requires java.desktop;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires javafx.media;
    requires javafx.web;
    requires jdk.jsobject;

    exports net.pan.textend;
}
