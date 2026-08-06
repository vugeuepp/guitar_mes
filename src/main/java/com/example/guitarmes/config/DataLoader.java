package com.example.guitarmes.config;

import org.springframework.boot.CommandLineRunner;

import com.example.guitarmes.entity.Body;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.entity.Neck;
import com.example.guitarmes.repository.BodyRepository;
import com.example.guitarmes.repository.GuitarRepository;
import com.example.guitarmes.repository.ManufacturingProcessRepository;
import com.example.guitarmes.repository.NeckRepository;

//@Component
public class DataLoader implements CommandLineRunner {
	private final GuitarRepository guitarRepository;
	private final ManufacturingProcessRepository manufacturingProcessRepository;
	private final NeckRepository neckRepository;
	private final BodyRepository bodyRepository;
	
	public DataLoader(
			GuitarRepository guitarRepository, 
			ManufacturingProcessRepository processRepository,
			NeckRepository neckRepository,
			BodyRepository bodyRepository) {
		this.guitarRepository = guitarRepository;
		this.manufacturingProcessRepository = processRepository;
		this.neckRepository = neckRepository;
		this.bodyRepository = bodyRepository;
	}
	
	@Override
	public void run(String... args) throws Exception {
		Guitar guitar1 = new Guitar("S0001", "ST-01", "塗装検品");
		Guitar guitar2 = new Guitar("S0002", "TL-01", "ネック取付");
		Guitar guitar3 = new Guitar("S0003", "LP-01", "パーツ取付");
		
		guitarRepository.save(guitar1);
		guitarRepository.save(guitar2);
		guitarRepository.save(guitar3);
		manufacturingProcessRepository.save(new ManufacturingProcess("塗装検品",1));
		manufacturingProcessRepository.save(new ManufacturingProcess("ネック取付",2));
		manufacturingProcessRepository.save(new ManufacturingProcess("パーツ取付",3));
		manufacturingProcessRepository.save(new ManufacturingProcess("調音",4));
		manufacturingProcessRepository.save(new ManufacturingProcess("最終検品",5));
		neckRepository.save(new Neck("N0001", "ST-01", "フレット打ち", "WORKING"));
		neckRepository.save(new Neck("N0002", "ST-01", "塗装", "WORKING"));
		neckRepository.save(new Neck("N0003", "TL-01", "検品", "WORKING"));
		bodyRepository.save(new Body("B0001", "ST-01", "Sunburst", "塗装", "WORKING"));
		bodyRepository.save(new Body("B0002", "ST-01", "Black", "乾燥", "WAITING"));
	}
}
