package cc.ivera.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ColorService {
    private final Random random = new Random();

    public List<String> getRandomColors(int count) {
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            colors.add(red + "," + green + "," + blue);
        }
        return colors;
    }
}