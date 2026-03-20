package cc.ivera.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import cc.ivera.dto.LocationDTO;
import cc.ivera.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.dto.QueryDTO;
import cc.ivera.vo.AjaxResult;
import cc.ivera.vo.LocationVo;

import java.util.List;

@RestController
public class LocationController {
    @Autowired
    private LocationService locationService;

    @GetMapping("/locationList")
    public AjaxResult locationList() {
        return AjaxResult.success(locationService.selectAllLocation());
    }

    @PostMapping("/addLocation")
    public AjaxResult addUser(@RequestBody LocationDTO location) {
        locationService.addLocation(location);
        return AjaxResult.success();
    }

    @PostMapping("/range")
    public AjaxResult range(@RequestBody QueryDTO vo) {
        PageHelper.startPage(1, 8);
        List<LocationVo> locationVos = locationService.selectByRange(vo);
        PageInfo pageInfo = new PageInfo(locationVos);
        return AjaxResult.success(pageInfo);
    }
}
