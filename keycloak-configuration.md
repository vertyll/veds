# Keycloak — Post-Startup Configuration

> **Keycloak 26.2.5** | Admin Console: http://localhost:9000/admin | Login: `admin` / `admin`

After `docker compose up`, the `veds` realm is automatically imported from `keycloak/realm-config/veds-realm.json`.  
The following steps need to be performed **once** after setting up a fresh environment.

---

## 1. Assign Service Account Roles

The `veds-service-account` client's service account needs `realm-management` roles to create and manage users in Keycloak.

1. Open http://localhost:9000/admin/master/console/#/veds/clients
2. Click **veds-service-account**
3. Go to the **Service account roles** tab
4. Click **Assign role**
5. Change the filter to **Filter by clients**
6. Search for `realm-management` and select:
   - `manage-users`
   - `view-users`
   - `query-users`
   - `manage-realm`
7. Click **Assign**

## 2. Verification

Get a service account token:

```bash
curl -s -X POST http://localhost:9000/realms/veds/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=veds-service-account&client_secret=veds-service-account-secret" \
  | python3 -m json.tool
```

This should return a JSON with an `access_token`.

List users (using the token from above):

```bash
TOKEN=$(curl -s -X POST http://localhost:9000/realms/veds/protocol/openid-connect/token \
  -d "grant_type=client_credentials&client_id=veds-service-account&client_secret=veds-service-account-secret" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:9000/admin/realms/veds/users | python3 -m json.tool
```

If you get a list (even an empty `[]`) — the service account is working correctly.  
If you get `403` — go back to step 1.
