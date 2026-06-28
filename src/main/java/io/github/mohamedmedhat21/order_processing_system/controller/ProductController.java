package io.github.mohamedmedhat21.order_processing_system.controller;

import io.github.mohamedmedhat21.order_processing_system.dto.PageResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.CreateProductRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.product.InventoryResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.ProductResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.UpdateProductRequest;
import io.github.mohamedmedhat21.order_processing_system.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

	private final ProductService productService;

	@GetMapping
	@Operation(summary = "List active products (paginated)")
	public PageResponse<ProductResponse> listProducts(
			@PageableDefault(size = 20) Pageable pageable
	) {
		return productService.listActiveProducts(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get product details")
	public ProductResponse getProduct(@PathVariable Long id) {
		return productService.getProduct(id);
	}

	@GetMapping("/{id}/inventory")
	@Operation(summary = "Check product inventory (always reads from database, never cache)")
	public InventoryResponse getInventory(@PathVariable Long id) {
		return productService.getInventory(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create product (admin)", security = @SecurityRequirement(name = "bearerAuth"))
	public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
		return productService.createProduct(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update product (admin)", security = @SecurityRequirement(name = "bearerAuth"))
	public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
		return productService.updateProduct(id, request);
	}
}
