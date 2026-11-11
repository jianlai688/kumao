package com.kumao.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kumao.common.result.R;
import com.kumao.common.security.UserContext;
import com.kumao.music.entity.Banner;
import com.kumao.music.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerMapper bannerMapper;

    @GetMapping
    public R<List<Banner>> list() {
        return R.ok(bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1).orderByAsc(Banner::getSort)));
    }

    @PostMapping("/admin")
    public R<Long> create(@RequestBody Banner banner) {
        UserContext.requireAdmin();
        bannerMapper.insert(banner);
        return R.ok(banner.getId());
    }

    @PutMapping("/admin/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Banner banner) {
        UserContext.requireAdmin();
        banner.setId(id);
        bannerMapper.updateById(banner);
        return R.ok();
    }

    @DeleteMapping("/admin/{id}")
    public R<Void> delete(@PathVariable Long id) {
        UserContext.requireAdmin();
        bannerMapper.deleteById(id);
        return R.ok();
    }
}
