
package cc.ivera.controller;

import cc.ivera.dto.PhotoTypeSaveDTO;
import cc.ivera.service.PhotoTypeService;
import cc.ivera.vo.PhotoTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/photoType")
@RequiredArgsConstructor
public class PhotoTypeController {

    private final PhotoTypeService service;

    @PostMapping("/save")
    public void save(@RequestBody PhotoTypeSaveDTO dto){
        service.save(dto);
    }

    @GetMapping("/tree")
    public List<PhotoTypeVO> tree(){
        return service.tree();
    }
}
