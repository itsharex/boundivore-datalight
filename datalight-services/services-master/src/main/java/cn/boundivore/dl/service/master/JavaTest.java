/**
 * Copyright (C) <2023> <Boundivore> <boundivore@foxmail.com>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Apache License, Version 2.0
 * as published by the Apache Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License, Version 2.0 for more details.
 * <p>
 * You should have received a copy of the Apache License, Version 2.0
 * along with this program; if not, you can obtain a copy at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package cn.boundivore.dl.service.master;

import cn.boundivore.dl.ssh.tools.SshTool;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Description: SSH 测试
 * Created by: Boundivore
 * E-mail: boundivore@foxmail.com
 * Creation time: 2023/6/30
 * Modification description:
 * Modified by:
 * Modification time:
 * Version: V1.0
 */
public class JavaTest {
    public static void main(String[] args) {
        try {
            SshTool sshTool = SshTool.getInstance();
            SSHClient sshClient = sshTool.connect(
                    "node03",
                    22,
                    "root",
                    "D:\\datalight\\z.node01\\id_rsa"
            );


//            String script = "#!/bin/bash\n" +
//                    "sleep 10\n" +
//                    "reboot -h now\n";
//            Session session = sshClient.startSession();
//            Session.Command cmd = session.exec("nohup bash -c \"" + script + "\" > /dev/null 2>&1 &");
//            cmd.join();
//
//            session.close();
//            sshClient.disconnect();

            SshTool.ExecResult execResult = sshTool.exec(sshClient, "/opt/datalight/node/scripts/action-restart.sh");
            System.out.println(execResult);
            sshTool.disconnect(sshClient);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
