# Inventory Request API

This module supports inter-branch inventory requests.

Use case:
- One branch can request stock from another branch.
- `ADMIN` and `SUPER_ADMIN` users can also create requests on behalf of branches.
- The supplying branch can accept or reject the request.
- Stock is moved between branch inventories only when the request is accepted.

## Roles and access rules

### `BRANCH_USER`
- Can create requests only for their own branch as the requesting branch.
- Can view requests only if their branch is either the requesting branch or the supplying branch.
- Can accept or reject only requests where their branch is the supplying branch.

### `ADMIN` and `SUPER_ADMIN`
- Can create requests for any branch.
- Can view requests for any branch.
- Can accept or reject requests for any branch.

## Status flow

- `PENDING`: newly created request
- `ACCEPTED`: supplying branch accepted and inventory was transferred
- `REJECTED`: supplying branch rejected and no inventory was transferred

Only `PENDING` requests can be processed.

## Inventory behavior

When a request is accepted:
- The system checks available quantity in the supplying branch.
- Available quantity is calculated as `onHand - reserved`.
- If stock is insufficient, the request cannot be accepted.
- Requested quantity is deducted from the supplying branch inventory.
- Requested quantity is added to the requesting branch inventory.

When a request is rejected:
- No inventory is moved.

## Endpoints

Base path: `/api/inventory-requests`

### 1. Create inventory request

`POST /api/inventory-requests`

Creates a new request from one branch to another.

#### Request body

```json
{
  "requestingBranchId": 2,
  "supplyingBranchId": 1,
  "requestNote": "Need fast-moving frames for weekend orders",
  "items": [
    {
      "variantId": 101,
      "quantity": 5
    },
    {
      "variantId": 205,
      "quantity": 2
    }
  ]
}
```

#### Validation rules

- `requestingBranchId` is required.
- `supplyingBranchId` is required.
- Requesting and supplying branch must be different.
- `items` must contain at least one item.
- `variantId` is required for each item.
- `quantity` must be greater than `0`.
- Duplicate `variantId` lines are not allowed.

#### Response example

```json
{
  "id": 1,
  "requestingBranchId": 2,
  "requestingBranchName": "Kandy Branch",
  "supplyingBranchId": 1,
  "supplyingBranchName": "Main Branch",
  "requestedByUserId": 7,
  "requestedByUsername": "branch_kandy",
  "processedByUserId": null,
  "processedByUsername": null,
  "status": "PENDING",
  "requestNote": "Need fast-moving frames for weekend orders",
  "decisionNote": null,
  "processedAt": null,
  "createdAt": "2026-04-07T01:00:00",
  "items": [
    {
      "id": 1,
      "variantId": 101,
      "productId": 15,
      "productName": "RayBan Frame",
      "sku": "FR-RB-001",
      "requestedQuantity": 5.00,
      "uomCode": "EA",
      "uomName": "Each"
    }
  ]
}
```

### 2. Get request by id

`GET /api/inventory-requests/{id}`

Returns a single inventory request with its items.

#### Example

`GET /api/inventory-requests/1`

### 3. Search inventory requests

`GET /api/inventory-requests`

Returns paginated requests.

#### Query parameters

- `branchId` optional
- `status` optional: `PENDING`, `ACCEPTED`, `REJECTED`
- `direction` optional: `INCOMING`, `OUTGOING`
- `page` optional, default `0`
- `size` optional, default `20`

#### Direction behavior

- `OUTGOING`: requests created by the branch
- `INCOMING`: requests sent to the branch for fulfillment

For `ADMIN` and `SUPER_ADMIN`:
- If `direction` is used, `branchId` must also be provided.

For `BRANCH_USER`:
- The user is always limited to their own branch.
- If `direction=OUTGOING`, results are filtered by the user branch as requesting branch.
- If `direction=INCOMING`, results are filtered by the user branch as supplying branch.

#### Example requests

`GET /api/inventory-requests?page=0&size=20`

`GET /api/inventory-requests?branchId=2&status=PENDING`

`GET /api/inventory-requests?branchId=1&direction=INCOMING&status=PENDING`

#### Response shape

```json
{
  "items": [],
  "totalCounts": 0,
  "page": 0,
  "size": 20,
  "totalPages": 0
}
```

### 4. Accept request

`POST /api/inventory-requests/{id}/accept`

Accepts a pending request and transfers inventory.

#### Request body

```json
{
  "decisionNote": "Approved based on current stock"
}
```

Request body is optional.

#### Example

`POST /api/inventory-requests/1/accept`

#### Business behavior

- Allowed only for pending requests.
- Checks stock in the supplying branch for each requested item.
- If any item does not have enough available stock, the request is not accepted.
- On success, status becomes `ACCEPTED` and inventory is moved.

### 5. Reject request

`POST /api/inventory-requests/{id}/reject`

Rejects a pending request without moving inventory.

#### Request body

```json
{
  "decisionNote": "Insufficient stock for transfer this week"
}
```

Request body is optional.

#### Example

`POST /api/inventory-requests/1/reject`

## Common error cases

### `400 Bad Request`
- Requesting branch and supplying branch are the same
- Duplicate variant lines are submitted
- Quantity is zero or negative
- Invalid `direction`
- `branchId` is missing when `direction` is used by admin users
- Request is already processed
- Insufficient stock in the supplying branch during acceptance

### `401 Unauthorized`
- User is not authenticated

### `403 Forbidden`
- Branch user tries to create a request for another branch
- Branch user tries to view a request outside their branch
- Branch user tries to process a request for another supplying branch

### `404 Not Found`
- Inventory request not found
- Requesting branch not found
- Supplying branch not found
- Product variant not found

## Notes for frontend integration

- Use the inventory API to show current stock before creating a request:
  - `GET /api/inventories`
  - `GET /api/inventories/branches/{branchId}`
- Use `direction=OUTGOING` for the requester view.
- Use `direction=INCOMING` for the supplying branch approval queue.
- There is currently no notification or alert mechanism in the backend.
  Receiving a request means fetching it via the API.
