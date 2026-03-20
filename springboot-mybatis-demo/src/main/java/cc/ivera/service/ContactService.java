package cc.ivera.service;

import cc.ivera.entity.Contact;
import cc.ivera.mapper.ContactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class ContactService {

    @Autowired
    private ContactMapper contactMapper;

    public List<Contact> findAll() {
        return contactMapper.list();
    }

    public Contact findById(Integer id) {
        return contactMapper.get(id);
    }

    public void save(Contact contact) {
        contactMapper.add(contact);
    }

    public void delete(Integer id) {
        contactMapper.delete(id);
    }


    public void update(Contact contact) {
        contactMapper.update(contact);
    }


}
