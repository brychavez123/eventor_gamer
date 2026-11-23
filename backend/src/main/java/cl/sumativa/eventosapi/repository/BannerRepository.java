package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActivoTrueOrderByOrdenAsc();
}
