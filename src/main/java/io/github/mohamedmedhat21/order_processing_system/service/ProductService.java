package io.github.mohamedmedhat21.order_processing_system.service;

import io.github.mohamedmedhat21.order_processing_system.config.CacheConfig;
import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.domain.Product;
import io.github.mohamedmedhat21.order_processing_system.dto.PageResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.CreateProductRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.product.InventoryResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.ProductResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.product.UpdateProductRequest;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final InventoryRepository inventoryRepository;

	@Transactional(readOnly = true)
	@Cacheable(
			cacheNames = CacheConfig.PRODUCTS_CACHE,
			key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize"
	)
	public PageResponse<ProductResponse> listActiveProducts(Pageable pageable) {
		Page<Product> page = productRepository.findByActiveTrue(pageable);
		return toPageResponse(page);
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfig.PRODUCT_BY_ID_CACHE, key = "#id")
	public ProductResponse getProduct(Long id) {
		return ProductResponse.from(findActiveProduct(id));
	}

	@Transactional(readOnly = true)
	public InventoryResponse getInventory(Long productId) {
		findActiveProduct(productId);
		Inventory inventory = inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory for product", productId));
		return new InventoryResponse(productId, inventory.getQuantityAvailable());
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_BY_ID_CACHE}, allEntries = true)
	public ProductResponse createProduct(CreateProductRequest request) {
		Product product = new Product();
		product.setName(request.name());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setActive(true);
		Product saved = productRepository.save(product);

		Inventory inventory = new Inventory();
		inventory.setProduct(saved);
		inventory.setQuantityAvailable(request.initialQuantity());
		inventoryRepository.save(inventory);

		return ProductResponse.from(saved);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_BY_ID_CACHE}, allEntries = true)
	public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (request.name() != null) {
			product.setName(request.name());
		}
		if (request.description() != null) {
			product.setDescription(request.description());
		}
		if (request.price() != null) {
			product.setPrice(request.price());
		}
		if (request.active() != null) {
			product.setActive(request.active());
		}

		return ProductResponse.from(productRepository.save(product));
	}

	private Product findActiveProduct(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		if (!product.isActive()) {
			throw new ResourceNotFoundException("Product", id);
		}
		return product;
	}

	private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
		return new PageResponse<>(
				page.getContent().stream().map(ProductResponse::from).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}
}
