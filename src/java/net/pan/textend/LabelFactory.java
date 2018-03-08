package net.pan.textend;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.Mnemonic;

class LabelFactory
{
    private static final String ORIGNAL_MNEMONIC_PARSING_VALUE = 
        LabelFactory.class.getPackage().getName() + ".originalMnemonicParsing";

    private LabelFactory()
    {
        // Deliberately empty.
    }

    static Label createLabel(String text,
                             Node target)
    {
        Label label = new Label(text);
        label.setMnemonicParsing(true);
        label.setLabelFor(target);
        label.setMinWidth(Region.USE_PREF_SIZE);

        return label;
    }

    private static void update(Mnemonic mnemonic,
                               Scene oldScene, 
                               Scene newScene)
    {
        if (oldScene != null)
        {
            oldScene.removeMnemonic(mnemonic);
        }
        if (newScene != null)
        {
            newScene.addMnemonic(mnemonic);
        }
    }

    static void saveAndDisableMnemonicParsing(Node node)
    {
        if (node instanceof Labeled)
        {
            Labeled labeled = (Labeled) node;
            if (node.getProperties().putIfAbsent(
                ORIGNAL_MNEMONIC_PARSING_VALUE,
                labeled.isMnemonicParsing()) == null)
            {
                labeled.setMnemonicParsing(false);
            }
        }
        if (node instanceof Parent)
        {
            for (Node child : ((Parent) node).getChildrenUnmodifiable())
            {
                saveAndDisableMnemonicParsing(child);
            }
        }
    }

    static void restoreMnemonicParsing(Node node)
    {
        if (node instanceof Labeled)
        {
            Object mnemonicParsing =
                node.getProperties().remove(ORIGNAL_MNEMONIC_PARSING_VALUE);
            if (mnemonicParsing instanceof Boolean)
            {
                ((Labeled) node).setMnemonicParsing((Boolean) mnemonicParsing);
            }
        }
        if (node instanceof Parent)
        {
            for (Node child : ((Parent) node).getChildrenUnmodifiable())
            {
                restoreMnemonicParsing(child);
            }
        }
    }
}
