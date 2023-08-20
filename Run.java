/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package run;


import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import java.util.concurrent.TimeUnit;
import java.util.stream.*;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTP;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

 
import javax.net.ssl.*;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 *
 * @author jack
 */
public class Run {

    private String PATH = "C:" + File.separator + "Users" + File.separator + "Public" + File.separator + "Documents" + File.separator + "roboj" + File.separator;  
    private String logline;
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"(.*?)\"");
    
    private static final String sshHost = "eushared15.twinservers.net";
    private static final String sshUser = "filmphot";
    private static final String sshPassword = "5yt76YG1jj";
    private static final int sshPort = 21098;
    
    private static final String mysqlHost = "localhost"; // SSH туннель будет перенаправлять на локальный порт
    private static final int mysqlPort = 3306; // Порт MySQL на удаленном сервере
    private static final String mysqlDatabase = "filmphot_stock";
    private static final String mysqlUser = "filmphot_all";
    private static final String mysqlPassword = "p#W5@CSN@q9i";
    
    //private static final String mysqlurl = "jdbc:mysql://ps1.thehost.com.ua:3306/stock";
    //private static final String mysqluser = "clouduserbd";
    //private static final String mysqlpassword = "TKjtOaHI123";
    // JDBC variables for opening and managing connection
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;  
    public String nameOfPc = getnameOfPc();
    private String[][] accountstmparr;
    Process process,process2;
    
  private Session sshSession;

  public Run() {
    if (checkFileExists(PATH + "lib", "jsch-0.1.55.jar")){   
        this.sshSession = createSshSession();
    }
  }
  
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception  {
    
        String PATH = "C:" + File.separator + "Users" + File.separator + "Public" + File.separator + "Documents" + File.separator + "roboj" + File.separator;
        String directoryName = PATH;
            File directory = new File(directoryName);
            if (! directory.exists()){
                directory.mkdir();      
            }



        Run run = new Run();
        String namepc = run.getnameOfPc();
        
        // Ожидание доступа к интернету
        waitForInternetAvailability();
        
        if (checkFileExists(PATH + "lib", "commons-net-3.8.0.jar")) 
            if (checkFileExists(PATH , namepc + "_run_err.txt")) {
                run.WriteLogLast(run.getlogline());
            }
        
        
        FileOutputStream f = new FileOutputStream(PATH  + namepc + "_run.txt");   
        System.setOut(new PrintStream(f));
        FileOutputStream ferr = new FileOutputStream(PATH + namepc + "_run_err.txt");
        System.setErr(new PrintStream(ferr));
        
        
        //if (!checkFileExists(PATH + "lib", "commons-net-3.8.0.jar")) {
            run.checkfiles();
        //}
        System.out.println("start checkfiles ");
        run.WriteLog(run.getlogline());
        
        System.setProperty("webdriver.chrome.driver", PATH + "src/workaround.bat");
    
        long acpower = 0, dcpower = 0; 
        System.out.println("get power conf ");
        run.WriteLog(run.getlogline());
        try {
            // Выполнение команды powercfg и получение вывода
            Process process = Runtime.getRuntime().exec("powercfg -q");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean groupsleep = false;
            while ((line = reader.readLine()) != null) {
                    String rline = reader.readLine();
                    // Поиск строки, содержащей информацию о режиме сна и времени до перехода в sleep
                    System.out.println("line: " + line);
                    System.out.println("rline: " + rline);
                    if (line.contains("Sleep after")) {
                        groupsleep = true;
                        //String guid = reedparameters(line);
                        String sleepModeLine = reader.readLine();
                        //System.out.println("Sleep Mode: " + sleepModeLine);
                        //break;
                    }
                    if(groupsleep && (rline.contains("Current AC Power"))){

                        String hexValue = rline.substring(rline.lastIndexOf("0x") + 2);
                         System.out.println("hexValue: " + hexValue );
                        if(hexValue.length() > 6){
                        acpower = Long.parseLong(hexValue, 16);
                         System.out.println("Current AC Power Setting seconds: " +acpower );
                        }else{ System.out.println("raw rline: " + rline ); }
                    }
                    if(groupsleep && (line.contains("Current DC Power"))){
                        String hexValue = line.substring(line.lastIndexOf("0x") + 2);
                        if(hexValue.length() > 6){
                        dcpower = Long.parseLong(hexValue, 16);
                         System.out.println("Current DC Power Setting seconds: " +dcpower );
                         }else{ System.out.println("raw line: " + line ); }
                        groupsleep = false;
                    }
                    //if(dcpower>acpower)
                    //    acpower = dcpower;
            }
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
      
      
        int batteryStatus = checkbatteryStatus(true);
        int k = 0;
        
        System.out.println("run.loaddata");
        run.WriteLog(run.getlogline());
        String[][] data = run.loaddata();
        if(Integer.parseInt(data[0][3]) == 1){
            //remove file run.jar
            String filePath = PATH + "run_v2.jar"; 
        
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Файл run удален успешно.");
                } else {
                    System.out.println("Не удалось удалить файл run.");
                }
            } else {
                System.out.println("Файл run не существует.");
            }
            
            
            //download new verson
            try {
                downloadFolder(run.connectftp(), run.getnameOfPc(), PATH);
                System.out.println("download custom files from serv");
            } catch(Exception e){
                //
            } finally {
                if (!checkFileExists(PATH + "lib", "commons-net-3.8.0.jar")) {
                    try {
                         downloadFolder(run.connectftp(), "DESKTOP-FCTCNM4", PATH);    
                         System.out.println("download DESKTOP-FCTCNM4 files");
                     }  catch(Exception e){}
               }
            }    
            run.WriteLog(run.getlogline());
        }        
        
        Win32IdleTime.State state = Win32IdleTime.State.UNKNOWN;
        DateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        ProcessBuilder builder,builder2;        
        int idleSecwait = 10 * 60; //wait after idle state 
        if(idleSecwait>Long.valueOf(acpower).intValue()){
            //
        }
        boolean idlestart = false;     
        int secondsleft = 0 ;
        if(Long.valueOf(acpower).intValue() > 0){
            secondsleft = Long.valueOf(acpower).intValue()-idleSecwait;
        }else{
            secondsleft = 1147483647;
        }
        for (;;) {
             int idleSec = Win32IdleTime.getIdleTimeMillisWin32() / 1000;

             Win32IdleTime.State newState =
                 idleSec < 30 ? Win32IdleTime.State.ONLINE :
                 idleSec > idleSecwait ? Win32IdleTime.State.AWAY : Win32IdleTime.State.IDLE;
             
             
             if (newState != state) {
                 state = newState;
                 k = 0;
                 
                  
                 System.out.println(dateFormat.format(new Date()) + " # " + state);

                 if (state == Win32IdleTime.State.AWAY && (batteryStatus != 1)) {
                    System.out.println("Activate the mouse wheel to change state!");                    
                    
                    try{
                        System.out.println("start jar file");
                        run.WriteLog(run.getlogline());
			List<String> command = new ArrayList<String>();
		     
                        command.add("java");
                        command.add("-jar");
                        command.add(PATH + "roboj_v2.jar");

                        builder = new ProcessBuilder(command);		    
                        run.process = builder.start();
                        //System.exit(0);
                    }catch(Exception e){
			System.out.println("Executer threw a Exception : " + e);
			e.printStackTrace();			
                    } 
                     try {
                    TimeUnit.SECONDS.sleep(3);
                        } catch (Exception ex) {} 
                    try{
			List<String> command = new ArrayList<String>();
		     
                        command.add("java");
                        command.add("-jar");
                        command.add(PATH + "roboadobe_v2.jar");

                        builder2 = new ProcessBuilder(command);		    
                        run.process2 = builder2.start();
                        //System.exit(0);
                    }catch(Exception e){
			System.out.println("Executer threw a Exception : " + e);
			e.printStackTrace();			
                    } 
                    
                 }
                 if (state == Win32IdleTime.State.ONLINE  ) {
                    destroyallproc(run.process); 
                    destroyallproc(run.process2);
                     try { TimeUnit.SECONDS.sleep(2);
                        } catch (Exception ex) {} 
                    destroyallprocChrome();
                    idlestart = false;
                }
                 
                if (state == Win32IdleTime.State.IDLE || (state == Win32IdleTime.State.AWAY) ) {
                    idlestart = true;
                    System.out.println("idlestart: " + true);
                    System.out.println("secondsleft: " + (secondsleft-idleSecwait));
                } 
        
             }
             
             if(idlestart){
                    secondsleft--;
                    if(secondsleft < 10){
                        System.out.println("secondsleft: < 10 ");
                        destroyallproc(run.process); 
                        destroyallproc(run.process2);
                         try { TimeUnit.SECONDS.sleep(2);
                            } catch (Exception ex) {} 
                        destroyallprocChrome();
                        secondsleft = Long.valueOf(acpower).intValue() - idleSecwait;
                        idlestart = false;
                    }
             }
             
             batteryStatus = checkbatteryStatus(false);
             //System.out.println("state + newState - " + state +" " + newState + " " + idleSec); 
             //check status
             //if status 0 then destroy process and start new
             //run.itertime = 4;
             if(k > 240){
                int status = run.checkStatus();
                if(status == 1){
                    //update status to 0
                    run.updateStatus(0);
                    k = 0;
                }else if (status  == 0){
                    // close all threds and run again 
                    destroyallproc(run.process);
                    destroyallproc(run.process2);
                    try { TimeUnit.SECONDS.sleep(2);
                        } catch (Exception ex) {} 
                    destroyallprocChrome();
                    state = Win32IdleTime.State.IDLE;
                    run.updateStatus(2);
                    /*java.awt.Robot robot = new java.awt.Robot();
                    robot.mouseWheel(-1);
                    robot.mouseWheel(1);*/
                    k = 0;                    
                }
             } 
             k++;
             try { Thread.sleep(1000); } catch (Exception ex) {}
         }

    }
    
    public static int checkbatteryStatus(boolean writelog){
        try {
            // Получение состояния батареи
            ProcessBuilder batteryProcessBuilder = new ProcessBuilder("C:\\Windows\\System32\\wbem\\wmic.exe", "path", "Win32_Battery", "get", "BatteryStatus");
            Process batteryProcess = batteryProcessBuilder.start();
            BufferedReader batteryReader = new BufferedReader(new InputStreamReader(batteryProcess.getInputStream()));

            String batteryLine;
            while ((batteryLine = batteryReader.readLine()) != null) {
                if(writelog)
                System.out.println("batteryLine: " + batteryLine);
                // Обработка вывода состояния батареи
                if (!batteryLine.isEmpty()){
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(batteryLine);
                    StringBuilder stringBuilder = new StringBuilder();

                    if (matcher.find()) {
                        String digits = matcher.group();
                        if(writelog)                
                        System.out.println("Battery Status: " + Integer.parseInt(digits));
                        return Integer.parseInt(digits);
                    }  
                }
            }

            batteryProcess.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    private static void destroyallproc(Process process){
        
        if(process != null){
            ProcessHandle processHandle = process.toHandle();
                        
            System.out.println("-----------------");
            //Optional<ProcessHandle> processHandle2 = ProcessHandle.allProcesses().findFirst();
            //processHandle2.ifPresent(proc -> proc.children().forEach(child -> System.out.println("PID: [ " + child.pid() + " ], Cmd: [ " + child.info().command() + " ]")));
      
            System.out.println("-----------------");
            Stream<ProcessHandle> descendants = ProcessHandle.current().descendants();
            descendants.filter(ProcessHandle::isAlive)
                .forEach(ph -> destroychildproc(ph.pid()));            
                        //destroyProcess(processHandle);               
        }
    }
    
    private static void destroychildproc(long pid){
        String cmd = "taskkill /F /PID " + pid;
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception ex) {}
    }
    
    private static void destroyallprocChrome(){
        

        try {
            
            String line;
            Process p = Runtime.getRuntime().exec("tasklist  /FI \"imagename eq chromedriver.exe\"");
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                    //System.out.println(line); //<-- Parse data here.

                Pattern pattern = Pattern.compile("\\s(\\d+) \\b");
                Matcher matcher = pattern.matcher(line);
                
                while (matcher.find()) {
                    String pline = line.substring(matcher.start(), matcher.end());
                    pline = pline.replaceAll("\\s+","");
                    System.out.println("pid " + pline);
                    destroychildproc(Long.parseLong(pline));
                }
            }
            input.close();
            
        } catch (Exception ex) {  
            ex.printStackTrace();
        }
                
    } 
    
    private static void destroyProcess(ProcessHandle processHandle) throws IllegalStateException {
      System.out.println("Ready to destroy Process with id: " + processHandle.pid());
      processHandle.destroy();
   }

   public String[][] loaddata(){
        StringBuilder images = new StringBuilder();    
        images.setLength(0);
        String accountstmparr[][] = null;
        System.out.println("-----"+ getnameOfPc());
        String query = "select * from machins_config where machinename = '"+getnameOfPc()+"'";
        
    Session sshSession = this.sshSession;
    if (sshSession != null) {
        try {
            while (!isSshSessionConnected(sshSession)) {
                this.sshSession = createSshSession();
                sshSession = this.sshSession;
                Thread.sleep(1000);  
            }
            
            try (Connection con = createDatabaseConnection(sshSession)) {

            // getting Statement object to execute query
            stmt = con.createStatement();

            // executing SELECT query
            rs = stmt.executeQuery(query);
            int k = 0;
            accountstmparr = new String [10][10];
            while (rs.next()) {
                accountstmparr[k][0]= rs.getString("uid");
                accountstmparr[k][1]= rs.getString("machinename");
                accountstmparr[k][2]= rs.getString("status");
                accountstmparr[k][3]= rs.getString("upgrade");      
                k++;
            }
           
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sshSession.disconnect();
        }
        }
 
        return accountstmparr;
    }  
    
   public int checkStatus(){

        int status = 0;
        System.out.println("-----"+ getnameOfPc());
        String query = "select * from machins_config where machinename = '"+getnameOfPc()+"'";
    
    Session sshSession = this.sshSession;
    if (sshSession != null) {
        try {
            while (!isSshSessionConnected(sshSession)) {
                this.sshSession = createSshSession();
                sshSession = this.sshSession;
                Thread.sleep(1000);  
            }
            
            try (Connection con = createDatabaseConnection(sshSession)) {

            // getting Statement object to execute query
            stmt = con.createStatement();

            // executing SELECT query
            rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                status = Integer.parseInt(rs.getString("status"));
            }
           
        } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sshSession.disconnect();
        }
        }
 
        return status;
    }  
   
    public void updateStatus(int result){
        String query = "UPDATE machins_config  SET `status` = "+result+" WHERE `machinename` = ?";
        String nameOfPc = Run.getnameOfPc(); 

    Session sshSession = this.sshSession;
    if (sshSession != null) {
        try {
            while (!isSshSessionConnected(sshSession)) {
                this.sshSession = createSshSession();
                sshSession = this.sshSession;
                Thread.sleep(1000);  
            }
            
            try (Connection con = createDatabaseConnection(sshSession)) {
          
                PreparedStatement stmt = con.prepareStatement(query);
                // opening database connection to MySQL server
                //con = DriverManager.getConnection(mysqlurl, mysqluser, mysqlpassword);

                // getting Statement object to execute query
                //stmt = con.createStatement();

                stmt.setString(1, nameOfPc);
                // executing SELECT query
                int update = stmt.executeUpdate();
                
                stmt = con.prepareStatement(query);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sshSession.disconnect();
        }
    }

    } 
       
       
    private FTPClient connectftp(){
        String server = "eushared15.twinservers.net";
        int port = 21;
        String user = "admin_b";
        String pass = "e22N6L79pU7q";
        
        FTPClient ftpClient = new FTPClient();   
        try {

            ftpClient.connect(server, port);
            boolean success = ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            showServerReply(ftpClient);
 
            if (!success) {
                System.out.println("Could not login to the server");                 
            } 
            // Changes working directory
            success = ftpClient.changeWorkingDirectory("/home/filmphot/subdomains/stock.ipolaroid.com.ua/sites/default/files/logs");
            showServerReply(ftpClient);            
               
            } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            
        }
        return ftpClient;
   }
    private static void closeftp(FTPClient ftpClient){
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
    }
   
    private static void downloadFolder(FTPClient ftpClient, String remotePath, String localPath) throws IOException {
        //localPath = localPath +remotePath; 
        System.out.println(
            "Downloading remote folder " + remotePath + " to " + localPath);
         
        //ftpClient.changeWorkingDirectory("DESKTOP-FCTCNM4");
      
        File directory = new File(localPath);
        if (! directory.exists()){
            directory.mkdir();
        }

        showServerReply(ftpClient);
        FTPFile[] remoteFiles = ftpClient.listFiles(remotePath);
        for (FTPFile remoteFile : remoteFiles)
        {
            if (!remoteFile.getName().equals(".") &&
                !remoteFile.getName().equals(".."))
            {
                String remoteFilePath = remotePath + "/" + remoteFile.getName();
                String localFilePath = localPath  + remoteFile.getName();
                if (remoteFile.isDirectory())
                {
                    new File(localFilePath).mkdirs();
                    downloadsubFolder(ftpClient, remoteFilePath, localFilePath);
                }
                else
                {
                    System.out.println(
                        "Downloading remote file " + remoteFilePath + " to " +
                        localFilePath);

                    OutputStream outputStream =
                        new BufferedOutputStream(new FileOutputStream(localFilePath));
                    if (!ftpClient.retrieveFile(remoteFilePath, outputStream))
                    {
                        System.out.println(
                            "Failed to download file " + remoteFilePath);
                    }
                    outputStream.close();
                }
            }
        }
        closeftp(ftpClient);
    }
    
    private static void downloadsubFolder(FTPClient ftpClient, String remotePath, String localPath) throws IOException {
        //localPath = localPath +remotePath; 
        System.out.println(
            "Downloading remote subfolder " + remotePath + " to " + localPath);
      
        File directory = new File(localPath);
        if (! directory.exists()){
            directory.mkdir();
        }

        showServerReply(ftpClient);
        FTPFile[] remoteFiles2 = ftpClient.listFiles(remotePath);
        for (FTPFile remoteFile : remoteFiles2)
        {
            if (!remoteFile.getName().equals(".") &&
                !remoteFile.getName().equals(".."))
            {
                String remoteFilePath = remotePath + "/" + remoteFile.getName();          
                String localFilePath = localPath + "\\" + remoteFile.getName();
                if (remoteFile.isDirectory())
                {
                    new File(localFilePath).mkdirs();
                    downloadsubFolder(ftpClient, remoteFilePath, localFilePath);
                }
                else
                {
                    System.out.println(
                        "Downloading remote file " + remoteFilePath + " to " +
                        localFilePath);

                    OutputStream outputStream =
                        new BufferedOutputStream(new FileOutputStream(localFilePath));
                    if (!ftpClient.retrieveFile(remoteFilePath, outputStream))
                    {
                        System.out.println(
                            "Failed to download file " + remoteFilePath);
                    }
                    outputStream.close();
                }
            }
        }
    }
    
    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: " + aReply);
            }
        }
    }
        
    public void checkfiles(){

        String commonsNetFileUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/lib/commons-net-3.8.0.jar";
        String sshFileUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/lib/jsch-0.1.55.jar";
        String mysqlConnectorFileUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/lib/mysql-connector-java-8.0.27.jar";        
        String runUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/run_v21.jar";
        String runbatUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/wint.bat";
        String vbsUrl = "http://stock.ipolaroid.com.ua/sites/default/files/DESKTOP-FCTCNM4/robo.bat";

        String targetFolder = PATH + "lib";
        
        String commonsNetFileName = getFileName(commonsNetFileUrl);
        String sshFileName = getFileName(sshFileUrl);
        String mysqlConnectorFileName = getFileName(mysqlConnectorFileUrl);
        String runUrlName = getFileName(runUrl);
        String vbsUrlName = getFileName(vbsUrl);
        String runbatUrlName = getFileName(runbatUrl);
        
        String appDataFolder = System.getenv("APPDATA");
        String startupFolder = appDataFolder + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";

        //if (!checkFileExists(startupFolder, vbsUrlName)) {
            downloadFile(vbsUrl, startupFolder, vbsUrlName);
        //}
        
        // Проверка наличия файла commons-net в текущем каталоге
        if (!checkFileExists(targetFolder, commonsNetFileName)) {
            downloadFile(commonsNetFileUrl, targetFolder, commonsNetFileName);
        }    
        if (!checkFileExists(targetFolder, sshFileName)) {
            downloadFile(sshFileUrl, targetFolder, sshFileName);
        }
        // Проверка наличия файла mysql-connector-java в текущем каталоге
        if (!checkFileExists(targetFolder, mysqlConnectorFileName)) {
            downloadFile(mysqlConnectorFileUrl, targetFolder, mysqlConnectorFileName);
        } 
       
        targetFolder = "C:" + File.separator + "Users" + File.separator + "Public" + File.separator + "Documents" + File.separator + "roboj" + File.separator;
        
        if (!checkFileExists(targetFolder, runUrlName)) {
            downloadFile(runUrl, targetFolder, runUrlName);
        } 
        /*if (!checkFileExists(targetFolder, runbatUrlName)) {
            downloadFile(runbatUrl, targetFolder, runbatUrlName);
        } */
        

       
    }   
    
    private static void downloadFile(String fileUrl, String targetFolder, String fileName) {
       try {
            // Создание объекта TrustManager, игнорирующего проверку сертификата
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            // Создание SSLContext и установка TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Создание объекта HostnameVerifier, принимающего все хосты
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            URL url = new URL(fileUrl);
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Создание папки, если она не существует
            Path folderPath = Paths.get(targetFolder);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Открытие входного потока и создание выходного файла
            try (BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
                 FileOutputStream outputStream = new FileOutputStream(targetFolder + File.separator + fileName)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                System.out.println("Файл успешно загружен и сохранен в папке \"" + targetFolder + "\".");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }
    
    private static boolean checkFileExists(String targetFolder, String fileName) {
        // Проверка наличия файла в папке
        Path filePath = Paths.get(targetFolder + File.separator + fileName);
        return Files.exists(filePath);
    }
    
    private static String getFileName(String fileUrl) {
        // Получение имени файла из URL
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }
    public void WriteLog(String logline) {
        String server = "eushared15.twinservers.net";
        int port = 21;
        String user = "admin_b";
        String pass = "e22N6L79pU7q";
        nameOfPc = getnameOfPc();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        String filename = formatter.format(date) + "-" + nameOfPc;
        FTPClient ftpClient = new FTPClient();
        try {
          ftpClient.connect(server, port);
          boolean success = ftpClient.login(user, pass);
          ftpClient.enterLocalPassiveMode();
          ftpClient.setFileType(2);
          File path = new File(PATH + "logs\\");
          File logFile = new File(PATH + nameOfPc + "_run.txt");
          File logErrFile = new File(PATH + nameOfPc + "_run_err.txt");
          File logFileshutter = new File(PATH + nameOfPc + ".txt");
          File logErrFileshutter = new File(PATH + nameOfPc + "_err.txt");
          File logFileadobe = new File(PATH + nameOfPc + "_adobe.txt");
          File logErrFileadobe = new File(PATH + nameOfPc + "_adobe_err.txt");
          //showServerReply(ftpClient);
          if (!success) {
            System.out.println("Could not login to the server");
            return;
          } 
          success = ftpClient.changeWorkingDirectory("/home/filmphot/subdomains/stock.ipolaroid.com.ua/sites/default/files/logs");
          InputStream inputStream = new FileInputStream(logFile);
          boolean done = ftpClient.storeFile(filename + "_run", inputStream);
          showServerReply(ftpClient);
          inputStream.close();
          inputStream = new FileInputStream(logErrFile);
          done = ftpClient.storeFile(filename + "_run_err", inputStream);
          showServerReply(ftpClient);
          inputStream = new FileInputStream(logFileshutter);
          done = ftpClient.storeFile(filename, inputStream);
          showServerReply(ftpClient);
          inputStream = new FileInputStream(logErrFileshutter);
          done = ftpClient.storeFile(filename + "_err", inputStream);
          showServerReply(ftpClient);
          inputStream = new FileInputStream(logFileadobe);
          done = ftpClient.storeFile(filename + "_adobe", inputStream);
          showServerReply(ftpClient);
          inputStream = new FileInputStream(logErrFileadobe);
          done = ftpClient.storeFile(filename + "_adobe_err", inputStream);
          showServerReply(ftpClient);
          inputStream.close();
        } catch (IOException ex) {
          System.out.println("Error: " + ex.getMessage());
          ex.printStackTrace();
        } finally {
          try {
            if (ftpClient.isConnected()) {
              ftpClient.logout();
              ftpClient.disconnect();
            } 
          } catch (IOException ex) {
            ex.printStackTrace();
          } 
        } 
      }

public void WriteLogLast(String logline) {
        String server = "eushared15.twinservers.net";
        int port = 21;
        String user = "admin_b";
        String pass = "e22N6L79pU7q";
        nameOfPc = getnameOfPc();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        String filename = formatter.format(date) + "-" + nameOfPc;
        FTPClient ftpClient = new FTPClient();
        try {
          ftpClient.connect(server, port);
          boolean success = ftpClient.login(user, pass);
          ftpClient.enterLocalPassiveMode();
          ftpClient.setFileType(2);
          File path = new File(PATH + "logs\\");
          File logFile = new File(PATH + nameOfPc + "_run.txt");
          File logErrFile = new File(PATH + nameOfPc + "_run_err.txt");
          File logFileshutter = new File(PATH + nameOfPc + ".txt");
          File logErrFileshutter = new File(PATH + nameOfPc + "_err.txt");
          File logFileadobe = new File(PATH + nameOfPc + "_adobe.txt");
          File logErrFileadobe = new File(PATH + nameOfPc + "_adobe_err.txt");
          //showServerReply(ftpClient);
          if (!success) {
            System.out.println("Could not login to the server");
            return;
          } 
          success = ftpClient.changeWorkingDirectory("/home/filmphot/subdomains/stock.ipolaroid.com.ua/sites/default/files/logs");
          InputStream inputStream = null;
          boolean done = false;
          try {
            inputStream = new FileInputStream(logFile);
            done = ftpClient.storeFile(filename + "_run_prev", inputStream);
            showServerReply(ftpClient);
            inputStream.close();
          } catch (IOException ex) {
            System.out.println("Error file _run_prev ");
            ex.printStackTrace();
          }
          try {
            inputStream = new FileInputStream(logErrFile);
            done = ftpClient.storeFile(filename + "_run_err_prev", inputStream);
            showServerReply(ftpClient);
          } catch (IOException ex) {
            System.out.println("Error file _run_err_prev ");
            ex.printStackTrace();
          }
          try {
            inputStream = new FileInputStream(logFileshutter);
            done = ftpClient.storeFile(filename + "_prev", inputStream);
            showServerReply(ftpClient);
          } catch (IOException ex) {
            System.out.println("Error file _prev ");
            ex.printStackTrace();
          }
          try {
            inputStream = new FileInputStream(logErrFileshutter);
            done = ftpClient.storeFile(filename + "_err_prev", inputStream);
            showServerReply(ftpClient);
          } catch (IOException ex) {
            System.out.println("Error file _err_prev ");
            ex.printStackTrace();
          }
          try {
            inputStream = new FileInputStream(logFileadobe);
            done = ftpClient.storeFile(filename + "_adobe_prev", inputStream);
            showServerReply(ftpClient);
          } catch (IOException ex) {
            System.out.println("Error file _adobe_prev ");
            ex.printStackTrace();
          }
          try {
          inputStream = new FileInputStream(logErrFileadobe);
            done = ftpClient.storeFile(filename + "_adobe_err_prev", inputStream);
            showServerReply(ftpClient);
            inputStream.close();
          } catch (IOException ex) {
            System.out.println("Error file _adobe_err_prev ");
            ex.printStackTrace();
          }
        } catch (IOException ex) {
          System.out.println("Error: " + ex.getMessage());
          ex.printStackTrace();
        } finally {
          try {
            if (ftpClient.isConnected()) {
              ftpClient.logout();
              ftpClient.disconnect();
            } 
          } catch (IOException ex) {
            ex.printStackTrace();
          } 
        } 
      }

    private static void waitForInternetAvailability() {
        while (!isInternetReachable()) {
            System.out.println("Internet is not available. Waiting for 1 minute...");
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static boolean isInternetReachable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return address.isReachable(5000); // Timeout of 5 seconds
        } catch (Exception e) {
            return false;
        }
    }
    
      public void setlogline(String logline) {
        this.logline += logline;
      }

      public String getlogline() {
        return this.logline;
      }

    public void setnameOfPc(String nameOfPc) {
        this.nameOfPc = nameOfPc;
    }
    public static String getnameOfPc() {
        String nameOfPc = "";
        java.net.InetAddress localMachine = null; 
        try {
            localMachine = java.net.InetAddress.getLocalHost();
            nameOfPc = localMachine.getHostName();
            // Удаление кириллических символов из имени хоста
            nameOfPc = nameOfPc.replaceAll("[^\\x00-\\x7F]", "");
        } catch(Exception e){}    
        
        return nameOfPc;
    }    
    
    
    private static Session createSshSession() {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            return session;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static Connection createDatabaseConnection(Session sshSession) {
        try {
            int assignedPort = sshSession.setPortForwardingL(0, mysqlHost, mysqlPort);

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setURL("jdbc:mysql://localhost:" + assignedPort + "/" + mysqlDatabase);
            dataSource.setUser(mysqlUser);
            dataSource.setPassword(mysqlPassword);

            return dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

  
  private static boolean isSshSessionConnected(Session session) {
    return session != null && session.isConnected();
  }
  
}
