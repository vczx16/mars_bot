package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import util.DHashUtil.*;

import javax.swing.*;

import static java.sql.ResultSet.*;
import static util.DHashUtil.getDHash;
import static util.DHashUtil.getHammingDistance;

public class MyBot extends TelegramLongPollingBot {

    private String token = "5703056762:AAEHuUA1gJUrbkBlGlkBa1df3nIDMJP--2E";
    private String botUsername = "vczx16_mars_bot";


    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) //Who are we sending a message to
                .text(what).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    private java.io.File downloadPhoto(final String filePath) {
        try {
            return downloadFile(filePath);
        } catch (final TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getFilePath(final PhotoSize photo) {
        final var filePath = photo.getFilePath();
        if (filePath != null && !filePath.isBlank()) {
            return filePath;
        }
        final GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(photo.getFileId());
        try {
            final org.telegram.telegrambots.meta.api.objects.File file = execute(getFileMethod);
            return file.getFilePath();
        } catch (final TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        var id = user.getId();
        var groupID = msg.getChatId();
        String messageText = update.getMessage().getText();
        int message_link = update.getMessage().getMessageId();
        // ??????????????????????????? id long message_group_link = update.getMessage().getChatId();
        /*List<PhotoSize> photos = update.getMessage().getPhoto();*/

        if (msg.isGroupMessage() || msg.isSuperGroupMessage()) {
            if (update.getMessage().hasText()) {
                if (messageText.contains("???")) {
                    sendText(groupID, "???");
                }
            }
            if (update.getMessage().hasPhoto()) {
                List<PhotoSize> photos = update.getMessage().getPhoto();
                PhotoSize photoSize = photos.stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElse(null);

                if (photoSize != null) {
                    // TODO download the photo
                    String filePath = getFilePath(photoSize);
                    java.io.File file = downloadPhoto(filePath);
                    java.io.File photo = new java.io.File("./" + file.getName() + ".jpg");
                    String picture_hash = getDHash(file);
                    System.out.println(picture_hash+" "+message_link);


                    final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
                    final String DB_URL = "jdbc:mysql://localhost:3306/bot_database";


                    final String USER = "root";
                    final String PASS = "";



                    Connection conn = null;
                    PreparedStatement stmt = null;
                    try {
                        // ?????? JDBC ??????
                        Class.forName(JDBC_DRIVER);

                        // ????????????
                        System.out.println("???????????????...");
                        conn = DriverManager.getConnection(DB_URL, USER, PASS);

                        // ???????????????????????????
                        String sql = "INSERT INTO mars_info_hash (id, message_link ,picture_hash) VALUES (?,?,?)";
                        stmt = conn.prepareStatement(sql);
                        stmt.setString(1, null);
                        stmt.setInt(2, message_link);
                        stmt.setString(3, picture_hash);



                        stmt.executeUpdate();

                        System.out.println("?????????????????????");
                        Statement stmt_extract = conn.createStatement();
                        ResultSet rs = stmt_extract.executeQuery("SELECT message_link,picture_hash FROM bot_database.mars_info_hash");

                        while (rs.next()) {

                            String data = rs.getString("picture_hash");
                            String picture_hash_link = rs.getString("message_link");
                            long distance = getHammingDistance(picture_hash, data);
                            if (distance<=5){
                            System.out.println("???????????????????????????????????????https://t.me/c/1576907774/"+picture_hash_link);
                            stmt.executeUpdate("UPDATE mars_info_hash SET message_link = ?, picture_hash = ?");
                            stmt.setInt(1,message_link);
                            stmt.setString(2,picture_hash);
                            break;
                            }else{
                                System.out.println("??????");
                            }
                            }


                    } catch (SQLException se) {
                        // ?????? JDBC ??????
                        se.printStackTrace();
                    } catch (Exception e) {
                        // ?????? Class.forName ??????
                        e.printStackTrace();
                    } finally {
                        // ????????????
                        try {
                            if (stmt != null) stmt.close();
                        } catch (SQLException se2) {
                        }
                        try {
                            if (conn != null) conn.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                    }


                    try {
                        try (
                                InputStream in = new BufferedInputStream(
                                        new FileInputStream(file));
                                OutputStream out = new BufferedOutputStream(
                                        new FileOutputStream(photo))) {

                            byte[] buffer = new byte[1024];
                            int lengthRead;
                            while ((lengthRead = in.read(buffer)) > 0) {
                                out.write(buffer, 0, lengthRead);
                                out.flush();
                            }
                        }
                    } catch (IOException e) {

                    }


                    System.out.println("Temporary file: " + file);

                }
            }


        }

        System.out.println("receive: " + messageText + " " + message_link);
    }
}
