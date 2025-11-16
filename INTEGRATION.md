# Guide d'intégration - Image Service

**Pour les développeurs de `user-service` et `shop-service`**

---

## Que stocker en base de données ?

### **Table `users` (user-service)**

```
    -- Photo de profil
    profile_image_id UUID,           -- ID dans le service image
    profile_image_url VARCHAR(500),  -- URL directe MinIO
    
    -- Bannière
    banner_image_id UUID,            -- ID dans le service image
    banner_image_url VARCHAR(500),   -- URL directe MinIO

```

### **Table `products` (shop-service)**

```
    -- Image principale du produit
    main_image_id UUID,              -- ID dans le service image
    main_image_url VARCHAR(500),     -- URL directe MinIO
```

**Pourquoi les 2 champs ?**
- `*_image_id` → Pour supprimer/gérer l'image via l'API
- `*_image_url` → Pour afficher rapidement côté frontend (pas besoin d'appel API)
---


