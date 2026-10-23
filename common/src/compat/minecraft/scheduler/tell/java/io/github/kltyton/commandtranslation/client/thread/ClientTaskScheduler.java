package io.github.kltyton.commandtranslation.client.thread;

import net.minecraft.client.Minecraft;

public final class ClientTaskScheduler {
    private ClientTaskScheduler() {}
    public static void enqueue(Runnable task) { Minecraft.getInstance().tell(task); }
}
