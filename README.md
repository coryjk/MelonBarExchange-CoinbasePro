# TODO:
* Track TODO in `Issues` tab
* Implement all APIs
* Separate core definitions into another package like `MelonBarCore` or `MelonBarCommons`

---------------------------------
## Example usage:

#### Initialize Coinbase Pro client:
```java
// barebone requirements for making Coinbase Pro API requests
final Authentication authentication = new CoinbaseProAuthentication(API_KEY, API_PASSWORD, API_SECRET);
final HttpClient httpClient = new HttpClientImpl(authentication, java.net.http.HttpClient.newHttpClient());
final Enricher requestEnricher = new RequestEnricher();

// API implementations
final AccountsApi accountsApi = new AccountsApiImpl(httpClient, requestEnricher);
final OrdersApi ordersApi = new OrdersApiImpl(httpClient, requestEnricher);
...

// init Coinbase Pro client
final CoinbaseProClient coinbaseProClient = new CoinbaseProClientImpl(accountsApi, ordersApi, ...);
```

#### Market order:
```java
// market order to buy 0.01 ETH using USD.
final MarketOrderRequest marketOrderRequest = MarketOrderRequest.builder()
                .orderSide(OrderSide.BUY)
                .product(Product.ETH_USD)
                .size(new BigDecimal("0.01"))
                .build();
final Response response = coinbaseProClient.placeMarketOrder(marketOrderRequest);
```

#### Limit order:
```java
// TODO
```

---------------------------------
## Current structure:

### 1. API Contract _(Request Level)_ 
_NOTE: Coinbase Pro API is well-documented online (see: [link](https://docs.pro.coinbase.com/))._

All Coinbase Pro API requests follow a strict format, with defined query parameters,
required fields, optional fields (with interrelated rules), etc. We will define an
API contract that strictly follows the documentation. All request constraints and API
contract validation will be handled at _request_ level.

The current strategy is to define each request with explicitly-designated required
fields with validation functions, which are composite predicates of the request's
required and optional fields. The authority of input/query validation definitions is on the request
class.

### 2. Execution _(Execution Level)_
Before dispatching a given request to Coinbase Pro, whether it be simply querying for
accounts, or submitting an ambitious limit buy, preliminary steps are required.

By definition, each request is enforced to have its own validation function. All request-level
validation steps will occur, along with any custom constraints, which may be injected by the
_user_. In addition, there is some degree of authentication required by Coinbase (e.g. message 
signing).

In short, execution level can be broken up into steps:
1. Request content validation and custom constraints check
2. Generate URI and request body, referred to as the _enrichment_ step
2. Authentication

### 3. HTTP Client _(Dispatch Level)_
Builds and sends the HTTP request, and handles building the HTTP response. Any further
evaluation of the response should be done as post-processing.