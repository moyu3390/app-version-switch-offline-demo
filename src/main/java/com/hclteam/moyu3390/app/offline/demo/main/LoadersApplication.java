package com.hclteam.moyu3390.app.offline.demo.main;

import com.hclteam.moyu3390.app.offline.demo.processor.AppProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.processor.ProcessorManager;
import com.hclteam.moyu3390.app.offline.demo.web.bean.vo.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {"com.hclteam.moyu3390.app.offline.demo"})
public class LoadersApplication implements CommandLineRunner {
    @Autowired
    ApplicationContext applicationContext;


    public static void main(String[] args) {
        SpringApplication.run(LoadersApplication.class, args);
        ApplicationHome home = new ApplicationHome();
        System.err.println(home.getDir());


    }

    @Override
    public void run(String... args) throws Exception {
        // 加载应用事务 方式①
        AppInfo appInfo1 = ProcessorManager.loadProcessor("testapp", ProcessorManager.jarDir, ProcessorManager.packagePath, "v1");
        ProcessorManager.setAppInfoIfAbsent(appInfo1);
        // 加载应用事务 方式②
        AppInfo appInfo = AppProcessorManager.loadProcessor("testapp", ProcessorManager.jarDir, ProcessorManager.packagePath, "v1");
        AppProcessorManager.setAppInfoIfAbsent(appInfo);


    }
}
