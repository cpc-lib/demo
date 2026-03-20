package cc.ivera.controller;

import cc.ivera.entity.Contact;
import cc.ivera.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @CrossOrigin
    @GetMapping("/phone")
    public String getPhone() {
        Contact contact = contactService.findById(Integer.valueOf(1));
        if(contact==null){
            return "18759077217";
        }else{
            return contact.getTelephone();
        }
    }


    @CrossOrigin
    @RequestMapping("/update")
    public String update(Contact contact) {
        contactService.update(contact);
        return "success";
    }

    @CrossOrigin
    @RequestMapping("/add")
    public String add(Contact contact) {
        contactService.save(contact);
        return "success";
    }



}
