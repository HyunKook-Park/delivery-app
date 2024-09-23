package com.sparta.tazzaofdelivery.domain.store.service;

import com.sparta.tazzaofdelivery.domain.exception.ErrorCode;
import com.sparta.tazzaofdelivery.domain.exception.TazzaException;
import com.sparta.tazzaofdelivery.domain.menu.dto.response.MenuSaveResponse;
import com.sparta.tazzaofdelivery.domain.menu.entity.Menu;
import com.sparta.tazzaofdelivery.domain.store.dto.request.StoreCreateRequest;
import com.sparta.tazzaofdelivery.domain.store.dto.response.StoreCreateResponse;
import com.sparta.tazzaofdelivery.domain.store.dto.response.StoreGetAllResponse;
import com.sparta.tazzaofdelivery.domain.store.dto.response.StoreGetResponse;
import com.sparta.tazzaofdelivery.domain.store.entity.Store;
import com.sparta.tazzaofdelivery.domain.store.enums.StoreStatus;
import com.sparta.tazzaofdelivery.domain.store.repository.StoreRepository;
import com.sparta.tazzaofdelivery.domain.user.entity.AuthUser;
import com.sparta.tazzaofdelivery.domain.user.entity.User;
import com.sparta.tazzaofdelivery.domain.user.enums.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;

    // 가게 생성
    public StoreCreateResponse createStore(StoreCreateRequest request, AuthUser authUser) {
        User user = User.fromAuthUser(authUser);

        if(!authUser.getUserRole().equals(UserType.OWNER)){
            throw new TazzaException(ErrorCode.STORE_FORBIDDEN);
        }
//
//        long currentStoreCount = storeRepository.countByUserAndStatus(user, StoreStatus.ACTIVE);
//        if(currentStoreCount >= 3){
//            throw new TazzaException(ErrorCode.STORE_BAD_REQUEST);
//        }

        Store newStore = new Store(
                request.getStoreName(),
                request.getCreatedAt(),
                request.getClosedAt(),
                request.getMinimumOrderQuantity(),
                request.getStoreAnnouncement(),
                user
        );
        Store savedStore = storeRepository.save(newStore);

        return new StoreCreateResponse(
                savedStore.getStoreName(),
                savedStore.getCreatedAt(),
                savedStore.getClosedAt(),
                savedStore.getMinimumOrderQuantity(),
                savedStore.getStoreAnnouncement()
        );
    }

    // 가게 다건 조회
    @Transactional(readOnly = true)
    public List<StoreGetAllResponse> getAllStores() {
        List<Store> stores = storeRepository.findAll();
        List<StoreGetAllResponse> storeList = new ArrayList<>();
        for (Store store : stores) {
            StoreGetAllResponse response = new StoreGetAllResponse(store.getStoreName(), store.getCreatedAt(), store.getClosedAt(), store.getStatus());
            storeList.add(response);
        }

        return storeList;
    }


    // 가게 단건 조회
    @Transactional(readOnly = true)
    public StoreGetResponse getStore(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new TazzaException(ErrorCode.STORE_NOT_FOUND));

        List<MenuSaveResponse> menuResponses = store.getMenus().stream()
                .filter(menu -> !menu.isDeleted())
                .map(MenuSaveResponse::new)
                .collect(Collectors.toList());

        return new StoreGetResponse(store.getStoreName(), menuResponses);

    }

    // 가게 폐업
    public void deleteStore(Long storeId, AuthUser authUser){
        Store store = storeRepository.findById(storeId).orElseThrow(()
                -> new TazzaException(ErrorCode.STORE_NOT_FOUND));

        if(!Objects.equals(authUser.getId(), storeId)){
            throw new TazzaException(ErrorCode.STORE_DELETE_FORBIDDEN);
        }
        store.setStatus(StoreStatus.CLOSED);
        storeRepository.save(store);

    }

}
