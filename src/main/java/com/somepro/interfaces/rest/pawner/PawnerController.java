package com.somepro.interfaces.rest.pawner;

import com.somepro.application.pawner.PawnerAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.pawner.converter.PawnerVoConverter;
import com.somepro.interfaces.rest.pawner.vo.PawnerCreateRequest;
import com.somepro.interfaces.rest.pawner.vo.PawnerDetailVO;
import com.somepro.interfaces.rest.pawner.vo.PawnerUpdateRequest;
import com.somepro.interfaces.rest.pawner.vo.PawnerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 当户档案接口（用户接口层）：只做协议适配 + VO 转换，业务在应用层。
 *
 * 路由：
 * - POST   /api/pawner              录入（编号后台生成，状态默认 NORMAL）
 * - PUT    /api/pawner/{id}         修改资料 / 冻结-恢复（不允许直接置 CLOSED）
 * - GET    /api/pawner/{id}         详情（含名下在押/在库件数、在当当票数；已注销也可查）
 * - POST   /api/pawner/{id}/close   注销（未结清挡回；注销后名册不可见、账保留）
 * - GET    /api/pawner/page         翻名册（姓名/身份证/电话/状态任意组合；全空调全量；恒不含 CLOSED）
 */
@RestController
@RequestMapping("/api/pawner")
public class PawnerController {

    private final PawnerAppService pawnerAppService;

    public PawnerController(PawnerAppService pawnerAppService) {
        this.pawnerAppService = pawnerAppService;
    }

    @PostMapping
    public Mono<Result<PawnerVO>> create(@Valid @RequestBody PawnerCreateRequest request) {
        return pawnerAppService.register(request.name(), request.idCard(), request.phone(), request.address())
                .map(PawnerVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<PawnerVO>> update(@PathVariable Long id,
                                         @Valid @RequestBody PawnerUpdateRequest request) {
        return pawnerAppService.modify(id, request.name(), request.idCard(), request.phone(),
                        request.address(), request.status())
                .map(PawnerVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<PawnerDetailVO>> detail(@PathVariable Long id) {
        return pawnerAppService.detail(id)
                .map(PawnerVoConverter::toDetailVo)
                .map(Result::ok);
    }

    @PostMapping("/{id}/close")
    public Mono<Result<Void>> close(@PathVariable Long id) {
        return pawnerAppService.close(id).then(Mono.just(Result.ok()));
    }

    @GetMapping("/page")
    public Mono<Result<PageVO<PawnerVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String idCard,
                                               @RequestParam(required = false) String phone,
                                               @RequestParam(required = false) String status) {
        return pawnerAppService.page(pageNum, pageSize, name, idCard, phone, status)
                .map(PawnerVoConverter::toPageVo)
                .map(Result::ok);
    }
}
