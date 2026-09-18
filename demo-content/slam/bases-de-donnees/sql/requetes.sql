SELECT id, titre, type_ressource, date_modification
FROM ressource
ORDER BY date_modification DESC;

SELECT matiere.nom, COUNT(ressource.id) AS nombre_ressources
FROM matiere
LEFT JOIN ressource ON ressource.matiere_id = matiere.id
GROUP BY matiere.id, matiere.nom
ORDER BY matiere.nom;
