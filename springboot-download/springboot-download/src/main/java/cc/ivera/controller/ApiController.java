package cc.ivera.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import cc.ivera.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@CrossOrigin
public class ApiController {

    @GetMapping("/api/get")
    @ResponseBody
    public ResponseEntity getInfo(HttpServletRequest request) {
        String name = request.getParameter("name");
        String age = request.getParameter("age");
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("name", name);
        map.put("age", age);
        return ok.body(map);
    }


    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity getUsers(HttpServletRequest request) {
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        List<User> userList = new ArrayList<>();
        User user = new User();
        user.setId("940829");
        user.setName("wiki");
        user.setPosition("leader");
        user.setAddtime(new Date());
        user.setAge(20);
        userList.add(user);
        Map<String, Object> map = new HashMap<>();
        map.put("status", 0);
        map.put("data", userList);
        return ok.body(map);
    }


    @GetMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity getUsers(@PathVariable("id") String id) {
        ResponseEntity.BodyBuilder ok = ResponseEntity.ok();
        User user = new User();
        user.setId(id);
        user.setName("wiki");
        user.setPosition("leader");
        user.setAddtime(new Date());
        user.setAge(20);
        Map<String, Object> map = new HashMap<>();
        map.put("status", 0);
        map.put("data", user);
        return ok.body(map);
    }


}
