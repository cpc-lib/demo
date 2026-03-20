package cc.ivera.test.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Demo97 {

    public static void main(String[] args) {
        Random random = new Random();
        Map<Integer, String> studentsMap = getStudents();
        Integer randomNumber = random.nextInt(studentsMap.size()) + 1;
        System.out.printf("%s: %s", "picked student", studentsMap.get(randomNumber));
    }


    private static Map<Integer, String> getStudents() {
        String[] list = {
                "Vera", "Adeline", "Herbert", "Tony", "Jack",
                "Halen", "Albert", "Roby", "Tom", "Jackson",
                "MJ", "Jeffy", "Taylor", "Sam", "Mike",
                "Robert", "Benjiamin"};

        Map<Integer, String> students = new HashMap<>();
        for (int i = 0; i < list.length; i++) {
            students.put(i + 1, list[i]);
        }

        return students;
    }

}
