package io.github.kltyton.commandtranslation.client.command;

/** Keeps the outgoing chat stream attached to its connection across player replacement. */
public interface CommandQueueOwner {
    CommandSendQueue commandTranslation$commandQueue();
}
