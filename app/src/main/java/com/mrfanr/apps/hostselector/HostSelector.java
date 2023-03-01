package com.mrfanr.apps.hostselector;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Host 选择器，选出最优的 host
 */
public class HostSelector {
    private static final int DEFAULT_PING_TIMEOUT = 500; // ping 超时时间
    private static final int DEFAULT_PING_TIMES = 3; // 单个 host ping的次数
    private final ExecutorService executorService;
    private final List<Host> hosts;

    public HostSelector(List<Host> hosts) {
        this.hosts = hosts;
        this.executorService = Executors.newFixedThreadPool(4, r -> new Thread(r, "host-selector-pool-" + r.hashCode()));
    }

    /**
     * 找到 ping 速度最快的 host
     */
    public Host bestHost() {
        try {
            List<Host> hostArray = pingHosts(this.hosts);
            if (hostArray == null || hostArray.isEmpty()) {
                return null;
            }
            printHosts(hostArray);
            return hostArray.get(0);
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    /**
     * 清理资源，关闭线程池
     */
    public void clear() {
        executorService.shutdown();
    }

    /**
     * ping 所有的 host，等待所有 ping 任务结束后，按照 pingTime 从小到大排序
     */
    private List<Host> pingHosts(List<Host> hosts) throws ExecutionException, InterruptedException {
        List<CompletableFuture<Host>> futures = hosts.stream().map(host -> pingAvgFuture(host, executorService)).collect(Collectors.toList());
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        CompletableFuture<List<Host>> result = allFuture.thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        hosts = result.get();
        Collections.sort(hosts);
        return hosts;
    }

    private CompletableFuture<Host> pingAvgFuture(Host host, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            host.pingTime = pingAvg(host, DEFAULT_PING_TIMES);
            return host;
        }, executorService);
    }

    /**
     * 单次 ping
     *
     * @return ping 消耗的时间
     */
    private long ping(Host host) {
        try {
            InetAddress address = InetAddress.getByName(host.host);
            long start = System.currentTimeMillis();
            boolean isReachable = address.isReachable(DEFAULT_PING_TIMEOUT);
            long end = System.currentTimeMillis();
            Log.d("HostSelector", "host: " + host + " reachable:" + isReachable + " time:" + (end - start));
            if (isReachable) {
                return end - start;
            }
            return Integer.MAX_VALUE;
        } catch (IOException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 多次 ping 之后取平均值
     *
     * @param times 次数
     * @return 多次 ping 平均值
     */
    private long pingAvg(Host host, int times) {
        long elapsedSum = 0;
        for (int i = 0; i < times; i++) {
            long elapsed = ping(host);
            elapsedSum += elapsed;
        }
        return (long) (elapsedSum / (times * 1.0));
    }

    private void printHosts(List<Host> hosts) {
        for (Host host : hosts) {
            Log.d("HostSelector", host.toString());
        }
    }
}

class Host implements Comparable<Host> {
    public String url;
    public String host;
    public Long pingTime;

    public Host(String url, String host) {
        this.url = url;
        this.host = host;
    }

    @Override
    public int compareTo(@NonNull Host another) {
        return (int) (this.pingTime - another.pingTime);
    }

    @NonNull
    @Override
    public String toString() {
        return "Host{" + "url='" + url + '\'' + ", host='" + host + '\'' + ", pingTime=" + pingTime + '}';
    }
}