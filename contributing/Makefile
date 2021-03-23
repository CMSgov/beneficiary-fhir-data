synthetic-data/*.rif:
	./fetch.sh
	
loadable:
	cd ../; git apply --ignore-space-change --ignore-whitespace contributing/patches/load_local_synthetic_data.patch
	cd ../; git apply --ignore-space-change --ignore-whitespace contributing/patches/docker_compose_db.patch

unloadable:
	cd ../; git apply -R contributing/patches/load_local_synthetic_data.patch
	cd ../; git apply -R contributing/patches/docker_compose_db.patch

up: .env
	docker-compose up -d
	docker-compose logs -f bfd

load: synthetic-data/*.rif
	docker-compose exec bfd ./load-data.sh

restart:
	docker-compose restart bfd; docker-compose logs -f | grep bfd

lint:
	docker-compose exec bfd ./lint.sh

.PHONY: loadable unloadable load
