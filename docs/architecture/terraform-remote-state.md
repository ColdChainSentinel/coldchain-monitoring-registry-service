# Estado remoto de Terraform — tarea pendiente

**Estado:** pendiente de asignación. **Responsable actual del estado:** quien desplegó el
entorno (`Nekolas777`), dueño de la suscripción Azure for Students. **Esfuerzo estimado:** 1 a 2 horas. **Costo:** centavos de USD al mes.

## Problema

El estado de Terraform del entorno `dev` (`infrastructure/terraform/terraform.tfstate`)
vive solo en la máquina de quien desplegó. Eso tiene tres consecuencias:

1. Nadie más del equipo puede ejecutar `terraform plan` o `apply` sobre el entorno real.
2. Si el archivo se pierde, Terraform deja de reconocer los recursos de Azure. Ya ocurrió
   una vez durante el despliegue y se recuperó de un plan guardado (ver
   [registro del despliegue](azure-deployment.md)).
3. El estado contiene en texto plano las contraseñas de PostgreSQL y la clave de la cuenta
   de almacenamiento de la Function. Hoy esa protección depende de un solo equipo personal.

## Objetivo

Guardar el estado en Azure Blob Storage con el backend `azurerm`, autenticación de
Microsoft Entra ID (sin claves de almacenamiento), bloqueo nativo por *lease* y versionado
del blob para poder recuperar versiones anteriores.

## Decisiones ya tomadas

- **Grupo de recursos separado** (`rg-coldchain-tfstate`): así `terraform destroy` del
  entorno nunca borra su propio estado.
- **Se crea con Azure CLI, no con este Terraform:** el backend tiene que existir antes de
  que Terraform pueda usarlo.
- **Región `brazilsouth`:** la política de la suscripción Azure for Students solo admite
  `brazilsouth`, `northcentralus`, `chilecentral`, `mexicocentral` y `westus`.
- **Sin claves compartidas:** `use_azuread_auth = true` y acceso por RBAC. El rol mínimo
  documentado por HashiCorp es **Storage Blob Data Contributor** sobre la cuenta o el
  contenedor.

## Reparto del trabajo

La migración final **solo puede hacerla quien tiene el estado actual**. Nunca enviar
`terraform.tfstate`, `terraform.tfvars` ni archivos `*.tfplan` por chat, correo o Git:
contienen credenciales.

La suscripción es personal del responsable: los demás integrantes no tienen acceso hasta
que él se lo otorgue, y solo un *Owner* puede asignar roles.

| Paso | Quién | Entregable |
|---|---|---|
| 0 | Responsable actual del estado | Grupo `rg-coldchain-tfstate` y acceso del integrante |
| 1 | Integrante asignado | Cuenta de almacenamiento y contenedor |
| 2 | Responsable actual del estado | Rol de datos para cada integrante que operará Terraform |
| 3 | Integrante asignado | PR con el bloque `backend` |
| 4-5 | Responsable actual del estado | Migración del estado y limpieza local |
| 6 | Integrante asignado | Verificación desde su propia máquina |

## Pasos

### 0. Dar acceso al integrante (responsable actual del estado)

El integrante debe pertenecer al mismo tenant (cuenta `@upc.edu.pe`).

```sh
SUB=$(az account show --query id -o tsv)
az group create -n rg-coldchain-tfstate -l brazilsouth
az role assignment create --role Contributor --assignee <correo-del-integrante> \
  --scope "/subscriptions/$SUB/resourceGroups/rg-coldchain-tfstate"
# Para que pueda ejecutar plan contra el entorno real (paso 6):
az role assignment create --role Reader --assignee <correo-del-integrante> \
  --scope "/subscriptions/$SUB/resourceGroups/rg-coldchain-dev"
```

`Reader` alcanza para `terraform plan`; dar `Contributor` sobre `rg-coldchain-dev` solo a
quien vaya a ejecutar `apply`.

### 1. Crear la cuenta de almacenamiento (integrante asignado)

El nombre de la cuenta debe ser único en Azure, de 3 a 24 caracteres en minúsculas y
números.

```sh
RG=rg-coldchain-tfstate
SA=stcoldchaintfstate$RANDOM   # anotar el nombre resultante
az storage account create -g $RG -n $SA -l brazilsouth \
  --sku Standard_LRS --kind StorageV2 --min-tls-version TLS1_2 \
  --allow-blob-public-access false --allow-shared-key-access false
az storage account blob-service-properties update -g $RG -n $SA \
  --enable-versioning true --enable-delete-retention true --delete-retention-days 14
# container-rm usa el plano de control: basta con Contributor (el rol de datos llega en el paso 2)
az storage container-rm create -g $RG --storage-account $SA -n tfstate
```

### 2. Dar acceso a los datos del estado (responsable actual del estado)

```sh
RG=rg-coldchain-tfstate
SA=<nombre-anotado-en-el-paso-1>
SCOPE=$(az storage account show -g $RG -n $SA --query id -o tsv)
az role assignment create --role "Storage Blob Data Contributor" \
  --assignee <correo-del-integrante> --scope "$SCOPE"
```

Repetir por cada integrante que operará Terraform, incluido el propio responsable: sin
ese rol nadie puede migrar ni leer el estado (el rol *Owner* no da acceso a los datos).

### 3. Abrir un PR con el backend

Agregar a `infrastructure/terraform/versions.tf`, dentro del bloque `terraform`:

```hcl
  backend "azurerm" {}
```

Y versionar `infrastructure/terraform/backend.hcl.example` con los valores no secretos:

```hcl
resource_group_name  = "rg-coldchain-tfstate"
storage_account_name = "<nombre-de-la-cuenta>"
container_name       = "tfstate"
key                  = "coldchain-dev.tfstate"
use_azuread_auth     = true
```

Agregar `backend.hcl` a `.gitignore` y documentar el cambio en el
[README de Terraform](../../infrastructure/terraform/README.md). **No ejecutar
`terraform apply` desde otra máquina antes del paso 4:** con un estado vacío, Terraform
intentaría crear recursos que ya existen.

### 4. Migrar el estado (responsable actual del estado)

Con el PR mezclado en la rama de trabajo y desde `infrastructure/terraform/`:

```sh
cp backend.hcl.example backend.hcl        # completar el nombre de la cuenta
cp terraform.tfstate ~/coldchain-tfstate-before-migration.json   # respaldo temporal
terraform init -migrate-state -backend-config=backend.hcl
terraform state list | wc -l              # debe dar 25
terraform plan                            # sin altas ni bajas
```

El único cambio aceptable en el plan es el diff cosmético conocido de
`transport = "HTTP"` en los probes (ver README de Terraform).

### 5. Limpiar copias locales (responsable actual del estado)

Una vez verificado el plan, borrar las copias locales con credenciales:
`terraform.tfstate`, `terraform.tfstate.backup`, `terraform.tfstate.broken-*`,
`terraform.tfstate.restored-backup`, todos los `*.tfplan` y el respaldo temporal del paso 4.
El versionado del blob reemplaza esos respaldos.

### 6. Verificar desde otra máquina (integrante asignado)

```sh
az login
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars   # pedir los valores al responsable, por canal privado
terraform init -backend-config=backend.hcl
terraform plan
```

## Criterios de aceptación

- [ ] `terraform plan` desde dos máquinas distintas muestra el mismo resultado, sin altas ni bajas.
- [ ] Un segundo `terraform plan` simultáneo falla con un error de *lease* mientras otro está en curso.
- [ ] La cuenta tiene `allowSharedKeyAccess = false`, sin acceso público a blobs y con versionado activo.
- [ ] No queda ningún `*.tfstate` ni `*.tfplan` en las máquinas de los integrantes.
- [ ] El README de Terraform explica cómo inicializar con `backend.hcl`.

## Problemas conocidos

- **`state blob is already locked`:** confirmar que nadie más está ejecutando Terraform y,
  recién entonces, liberar el *lease* desde el portal (botón *Break lease* del blob) o con
  `terraform force-unlock <id>`.
- **Error 403 al inicializar:** falta el rol del paso 2 o todavía no se propagó (puede tardar
  unos minutos).
