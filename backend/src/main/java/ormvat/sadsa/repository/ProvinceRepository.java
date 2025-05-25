package ormvat.sadsa.repository;
import ormvat.sadsa.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProvinceRepository extends JpaRepository<Province, Long> {
    Province findByDesignation(String designation);
}