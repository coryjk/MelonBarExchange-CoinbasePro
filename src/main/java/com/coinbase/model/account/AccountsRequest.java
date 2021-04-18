package com.coinbase.model.account;

import com.coinbase.annotation.RequestField;
import com.coinbase.model.request.BaseRequest;
import lombok.Builder;
import lombok.Getter;

/**
 * Requests for Coinbase Pro accounts API.
 */
@Getter
@Builder
public class AccountsRequest extends BaseRequest {

    /**
     * Account ID.
     */
    @RequestField(index = 0)
    private final String accountId;
}
