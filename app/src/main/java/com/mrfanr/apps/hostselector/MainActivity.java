package com.mrfanr.apps.hostselector;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private static final String[] urls = new String[]{
            "https://www.baidu.com",
            "https://www.toutiao.com",
            "https://www.douyin.com",
            "https://weixin.qq.com",
            "https://juejin.cn",
            "https://www.qq.com",
            "http://www.google.com",
            "https://www.taobao.com",
            "https://bitizen.org",
            "https://www.aliyun.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.detect_btn).setOnClickListener(v -> {
            new Thread(() -> {
                // urls 转化为 hosts，并过滤格式不合法的 url
                final List<Host> hosts = Arrays.stream(urls).map(url -> {
                    try {
                        return new Host(url, new URL(url).getHost());
                    } catch (MalformedURLException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                HostSelector hostSelector = new HostSelector(hosts);
                Host bestHost = hostSelector.bestHost();
                hostSelector.clear();

                runOnUiThread(() -> {
                    TextView textView = findViewById(R.id.best_url_text);
                    if (bestHost != null) {
                        textView.setText("最优的 Host：\n" + bestHost.url);
                    } else {
                        textView.setText("未找到合适的 Host");
                    }
                });
            }).start();
        });
    }
}