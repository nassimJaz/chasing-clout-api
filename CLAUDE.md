@README.MD
@DEVELOPPEMENT.md

## Règles de travail
- Avancer phase par phase (section « Avancement » de DEVELOPPEMENT.md) ; à la fin d'une phase, mettre à jour cette section.
- Toute modif de schéma passe par `initdb/*.sql` (jamais `ddl-auto=update`), puis vérifier que Spring démarre en `validate`.
- Suivre le style existant : packages `models` / `DAO` / `services` / `controllers` / `DTO`, pas de Lombok.
- Le front Angular est dans le repo voisin `../chasing-clout-front`.
