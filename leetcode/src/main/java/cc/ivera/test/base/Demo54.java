package cc.ivera.test.base;

import cc.ivera.util.IdUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Demo54 {
    public static void main(String[] args) {
        String filePath = "D:\\id.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (int i = 0; i < 703; i++) {
                String strId = IdUtil.getStrId();
                writer.write(strId);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

