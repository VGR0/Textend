package net.pan.textend;

import javafx.scene.Node;
//import javafx.scene.layout.AnchorPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;

class SampleField
{
    //private final AnchorPane pane;
    private final ScrollPane pane;

    final WebView view;

    SampleField()
    {
        //this.pane = new AnchorPane();
        this.view = new WebView();
        this.pane = new ScrollPane(view);
        this.pane.setFitToWidth(true);
        this.pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        this.view.setContextMenuEnabled(false);

        this.pane.setPrefViewportWidth(1);
        this.pane.setPrefViewportHeight(1);

        this.pane.setOnKeyPressed(this::suppressNavigation);
    }

    void bindSizeTo(Region node)
    {
        // TODO: Why don't these work?
        //pane.prefViewportWidthProperty().bind(node.widthProperty());
        //pane.prefViewportHeightProperty().bind(node.heightProperty());
        node.widthProperty().addListener((obs, old, width) -> {
            pane.setPrefViewportWidth(width.doubleValue());
        });
        node.heightProperty().addListener((obs, old, height) -> {
            pane.setPrefViewportHeight(height.doubleValue());
        });
    }

    private void suppressNavigation(KeyEvent event)
    {
        if (event.getCode().isNavigationKey()) 
        {
            event.consume();
        }
    }

    Region getNode()
    {
        return pane;
    }
}
