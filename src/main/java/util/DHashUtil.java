package util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.example.*;
import org.example.MyBot.*;
/**
 * 差异哈希算法
 */
public class DHashUtil {
    /**
     * 计算dHash方法
     *
     * @param file 文件
     * @return hash
     */
    public static String getDHash(File file) {
        //读取文件
        BufferedImage srcImage;
        try {
            srcImage = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        //文件转成9*8像素，为算法比较通用的长宽
        BufferedImage buffImg = new BufferedImage(9, 8, BufferedImage.TYPE_INT_RGB);
        buffImg.getGraphics().drawImage(srcImage.getScaledInstance(9, 8, Image.SCALE_SMOOTH), 0, 0, null);

        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        int[][] grayPix = new int[width][height];
        StringBuffer figure = new StringBuffer();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //图片灰度化
                int rgb = buffImg.getRGB(x, y);
                int r = rgb >> 16 & 0xff;
                int g = rgb >> 8 & 0xff;
                int b = rgb & 0xff;
                int gray = (r * 30 + g * 59 + b * 11) / 100;
                grayPix[x][y] = gray;

                //开始计算dHash 总共有9*8像素 每行相对有8个差异值 总共有 8*8=64 个
                if (x != 0) {
                    long bit = grayPix[x - 1][y] > grayPix[x][y] ? 1 : 0;
                    figure.append(bit);
                }
            }
        }

        return figure.toString();
    }

    /**
     * 计算海明距离
     * <p>
     * 原本用于编码的检错和纠错的一个算法
     * 现在拿来计算相似度，如果差异值小于一定阈值则相似，一般经验值小于5为同一张图片
     *
     * @param str1
     * @param str2
     * @return 距离
     */
    public static long getHammingDistance(String str1, String str2) {
        int distance;
        if (str1 == null || str2 == null || str1.length() != str2.length()) {
            distance = -1;
        } else {
            distance = 0;
            for (int i = 0; i < str1.length(); i++) {
                if (str1.charAt(i) != str2.charAt(i)) {
                    distance++;
                }
            }
        }
        return distance;
    }

    //DHashUtil 参数值为待处理文件夹
    public static void main(String[] args) {
       /* File file1 = new File("./1.jpg");
        File file2 = new File("./2.jpg");

        System.out.println("图片1hash值："+getDHash(file1));
        System.out.println("图片2hash值："+getDHash(file2));
        System.out.println("海明距离为："+getHammingDistance(getDHash(file1),getDHash(file2)));*/

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isFile() && file.getName().endsWith(".jpg")) {
                    return true;
                }
                return false;
            }
        };

        File dir = new File("./");
        File[] files = dir.listFiles(filter);
        for (File file : files) {
            String picture_hash = getDHash(file);

            System.out.println(picture_hash);

            final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
            final String DB_URL = "jdbc:mysql://localhost:3306/bot_database";


            final String USER = "root";
            final String PASS = "legend4008823823";


            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                // 注册 JDBC 驱动
                Class.forName(JDBC_DRIVER);

                // 打开链接
                System.out.println("连接数据库...");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);

                // 执行插入数据的操作
                System.out.println("向数据库中插入数据...");
                String sql = "INSERT INTO mars_info_hash (id, message_link ,picture_hash) VALUES (?,?,?)";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, null);
                stmt.setString(2, null);
                stmt.setString(3, picture_hash);


                stmt.executeUpdate();

                System.out.println("数据插入成功！");
            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            } catch (Exception e) {
                // 处理 Class.forName 错误
                e.printStackTrace();
            } finally {
                // 关闭资源
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

        }
    }
}
