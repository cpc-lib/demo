package cc.ivera.controller;

import cc.ivera.dto.ProductCreateRequest;
import cc.ivera.dto.ProductStatusRequest;
import cc.ivera.dto.ProductUpdateRequest;
import cc.ivera.dto.StockAdjustmentRequest;
import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.handler.GlobalExceptionHandler;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.ProductAdminService;
import cc.ivera.vo.ProductAdminView;
import cc.ivera.vo.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProductControllerTest {

    private ProductAdminService productAdminService;
    private AdminProductController controller;
    private AuthUser admin;

    @BeforeEach
    void setUp() {
        productAdminService = mock(ProductAdminService.class);
        controller = new AdminProductController(productAdminService);
        admin = new AuthUser(9L, "admin", UserRole.ADMIN);
        AuthContext.setUser(admin);
    }

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminListUsesViewWithComputedTotalStock() {
        Product product = product(7L, 5, 3, 4, 2);
        when(productAdminService.listAdmin()).thenReturn(Collections.singletonList(product));

        R<Map<String, Object>> response = controller.list();

        List<ProductAdminView> list = (List<ProductAdminView>) response.getData().get("list");
        assertEquals(1, list.size());
        ProductAdminView view = list.get(0);
        assertEquals(7L, view.getId());
        assertEquals(ProductStatus.ON_SHELF, view.getStatus());
        assertEquals(5, view.getAvailableStock());
        assertEquals(3, view.getLockedStock());
        assertEquals(4, view.getSoldStock());
        assertEquals(12L, view.getTotalStock());
        assertEquals(2, view.getVersion());
        verify(productAdminService).listAdmin();
    }

    @Test
    void userCannotCallAdminControllerEvenWithoutInterceptors() {
        AuthContext.setUser(new AuthUser(10L, "user", UserRole.USER));

        assertThrows(ForbiddenException.class, controller::list);

        verifyNoInteractions(productAdminService);
    }

    @Test
    void writeAndOperationEndpointsDelegateWithCurrentAdmin() {
        ProductCreateRequest createRequest = new ProductCreateRequest();
        createRequest.setTitle("新商品");
        createRequest.setPrice(100);
        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setTitle("改名商品");
        updateRequest.setPrice(200);
        updateRequest.setVersion(1);
        ProductStatusRequest statusRequest = new ProductStatusRequest();
        statusRequest.setStatus(ProductStatus.ON_SHELF);
        statusRequest.setVersion(2);
        StockAdjustmentRequest adjustmentRequest = new StockAdjustmentRequest();
        adjustmentRequest.setRequestId("request-1");
        adjustmentRequest.setDelta(3);
        adjustmentRequest.setReason("盘点补货");

        Product created = product(7L, 0, 0, 0, 0);
        Product updated = product(7L, 0, 0, 0, 2);
        Product changed = product(7L, 0, 0, 0, 3);
        InventoryOperation operation = new InventoryOperation();
        operation.setId(11L);
        List<InventoryOperation> operations = Arrays.asList(operation);
        when(productAdminService.create(createRequest, admin)).thenReturn(created);
        when(productAdminService.update(7L, updateRequest, admin)).thenReturn(updated);
        when(productAdminService.changeStatus(7L, statusRequest, admin)).thenReturn(changed);
        when(productAdminService.adjustStock(7L, adjustmentRequest, admin)).thenReturn(operation);
        when(productAdminService.listOperations(7L)).thenReturn(operations);

        assertEquals(7L, controller.create(createRequest).getData().getId());
        assertEquals(2, controller.update(7L, updateRequest).getData().getVersion());
        assertEquals(ProductStatus.ON_SHELF,
                controller.changeStatus(7L, statusRequest).getData().getStatus());
        assertSame(operation, controller.adjustStock(7L, adjustmentRequest).getData());
        assertSame(operations, controller.listOperations(7L).getData());

        verify(productAdminService).create(createRequest, admin);
        verify(productAdminService).update(7L, updateRequest, admin);
        verify(productAdminService).changeStatus(7L, statusRequest, admin);
        verify(productAdminService).adjustStock(7L, adjustmentRequest, admin);
        verify(productAdminService).listOperations(7L);
    }

    @Test
    void statusRouteUsesPatchAndInvalidCreateUsesHttp400() throws Exception {
        Product changed = product(7L, 1, 0, 0, 3);
        when(productAdminService.changeStatus(eq(7L), org.mockito.ArgumentMatchers.any(), eq(admin)))
                .thenReturn(changed);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(patch("/api/admin/products/7/status")
                        .contentType("application/json")
                        .content("{\"status\":\"ON_SHELF\",\"version\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_SHELF"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType("application/json")
                        .content("{\"title\":\" \",\"price\":100,\"initialStock\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(patch("/api/admin/products/7/status")
                        .contentType("application/json")
                        .content("{\"status\":\"UNKNOWN\",\"version\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void controllerDoesNotExposeDeleteOperation() {
        boolean hasDelete = Arrays.stream(AdminProductController.class.getDeclaredMethods())
                .map(Method::getAnnotations)
                .flatMap(Arrays::stream)
                .anyMatch(annotation -> annotation.annotationType() == DeleteMapping.class);

        assertFalse(hasDelete);
    }

    @Test
    void controllerDoesNotOverrideGlobalCorsAllowlist() {
        assertNull(AdminProductController.class.getAnnotation(CrossOrigin.class));
    }

    private Product product(Long id, int available, int locked, int sold, int version) {
        Product product = new Product();
        product.setId(id);
        product.setTitle("商品" + id);
        product.setPrice(1000);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setAvailableStock(available);
        product.setLockedStock(locked);
        product.setSoldStock(sold);
        product.setVersion(version);
        return product;
    }
}
