package com.vincent.learning.token.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExchangeTokenRequest {
    private String grantType;
    private String username;
    private String password;
    private String refreshToken;

    /**
     * use to convert the request grant_type to grantType
     *
     * @param grantType
     */
    public void setGrant_type(String grantType) { // NOSONAR
        setGrantType(grantType);
    }

    /**
     * use to convert the request refresh_token to refreshToken
     *
     * @param refreshToken
     */
    public void setRefresh_token(String refreshToken) { // NOSONAR
        setRefreshToken(refreshToken);
    }
}
