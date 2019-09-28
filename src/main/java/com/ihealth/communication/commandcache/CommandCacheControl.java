package com.ihealth.communication.commandcache;

import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.utils.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by jing on 16/10/11.
 */

public class CommandCacheControl {

    private String mac;
    private String type;
    private Queue<CommandCache> commandCacheQueue = new LinkedList<>();

    // gao 设为公有， 加set,get方法
    public class CommandCache {
        private List<String> actions = new ArrayList<>();
        private CommandCacheInterface listener;
        private long executeDelay = 2000;

        public CommandCache(List<String> actions, long executeDelay, CommandCacheInterface listener) {
            this.actions = actions;
            this.listener = listener;
            this.executeDelay = executeDelay;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> actions) {
            this.actions = actions;
        }

        public CommandCacheInterface getListener() {
            return listener;
        }

        public void setListener(CommandCacheInterface listener) {
            this.listener = listener;
        }

        public long getExecuteDelay() {
            return executeDelay;
        }

        public void setExecuteDelay(long executeDelay) {
            this.executeDelay = executeDelay;
        }
    }

    /**
     * Init
     *
     * @param mac
     * @param type
     */
    public CommandCacheControl(String mac, String type) {
        this.mac = mac;
        this.type = type;
    }

    /**
     * Cache command for InsSet
     *
     * @param actions
     * @param executeDelay
     * @param listener
     */
    public void commandExecuteInsSet(List<String> actions, long executeDelay, CommandCacheInterface listener) {
        Log.v("aa", " 执行 commandExecuteInsSet =" + actions.get(0));
        // 初始化cache , 如果队列为空，则加入，并执行，
        CommandCache commandCache = new CommandCache(actions, executeDelay, listener);
        if (commandCacheQueue.isEmpty()) {
            commandCacheQueue.offer(commandCache);
            //Execute InsSet    gao 回调发送指令
            listener.commandListener();
        } else {
            commandCacheQueue.offer(commandCache);
        }
    }

    /**
     * Receive action from {@link iHealthDevicesCallback#onDeviceNotify(String, String, String, String)}, then execute the next command.
     *
     * @param action
     */
    public void commandExecuteAction(String action) {
        // 执行下一个指令之前，先匹配顶部指令，是否和之前回调指令相同
        CommandCache commandCache = commandCacheQueue.peek();
        if (commandCache != null) {
            if (commandCache.getActions().contains(action)) {
                // 如相同，则移除，并取下一条指令执行
                commandCacheQueue.poll();
                commandExcute(commandCache);
            } else {
                // 如果没有匹配到Action， 则直接执行队顶指令
                commandExcute(commandCache);
            }
        }
    }


    // 异常情况，再次执行本条指令
    public void commandRepeatAction(String action) {
        CommandCache commandCache;
        Iterator<CommandCache> it = commandCacheQueue.iterator();
        while (it.hasNext()){
            commandCache = it.next();
            if(commandCache.getActions().contains(action)){
                CommandCacheInterface listener = commandCache.getListener();
                if (listener != null) {
                    listener.commandListener();
                } else {
                    // 容错
                }
            }
        }
    }

    // 执行队列顶部指令
    public void commandExcute(CommandCache commandCache){
        if (commandCacheQueue.size() > 0) {
            Log.v("aa", "队里里还有[" + commandCacheQueue.size() + "]条指令");
            commandCache = commandCacheQueue.peek();
            CommandCacheInterface listener = commandCache.getListener();
            if (listener != null) {
                listener.commandListener();
            } else {
                // 容错
            }
        } else {
            Log.v("aa", "队里里全部执行完毕");
        }
    }


    /**
     * Clear all the cache, eg. Device disconnect from {@link iHealthDevicesCallback#onDeviceConnectionStateChange(String, String, int, int, Map)}
     */

    public void commandClearCache() {
        if (!commandCacheQueue.isEmpty()) {
            commandCacheQueue.clear();
        }
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // gao   增加 队列的set\get 方法
    public Queue<CommandCache> getCommandCacheQueue() {
        return commandCacheQueue;
    }

    public void setCommandCacheQueue(Queue<CommandCache> commandCacheQueue) {
        this.commandCacheQueue = commandCacheQueue;
    }
}
