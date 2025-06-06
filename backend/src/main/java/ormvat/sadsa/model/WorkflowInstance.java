package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_dossier")
    private Long idDossier;

    @Column(name = "id_etape")
    private Long idEtape;

    @Column(name = "date_entree")
    private LocalDateTime dateEntree;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}

