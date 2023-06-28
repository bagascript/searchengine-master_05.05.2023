package searchengine.model.entity;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "search_index")
@NoArgsConstructor
@AllArgsConstructor
public class IndexEntity implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne()
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private LemmaEntity lemma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private PageEntity page;

    @Column(name = "lemmas_quantity", nullable = false)
    private float rank;
}
