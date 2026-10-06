"""Unit tests for the enhanced AnimalShelter data-access layer.

These tests are TF-free and require no live MongoDB: they inject a
``mongomock.MongoClient`` into a dedicated test database/collection, so they run
anywhere pytest + mongomock are installed. Run with::

    pytest -q
"""

import mongomock
import pytest

from animal_shelter import AnimalShelter, ValidationError


SAMPLE = [
    {"animal_type": "Dog", "breed": "Labrador Retriever Mix", "outcome_type": "Adoption",
     "datetime": "2017-01-05 10:00:00", "name": "Rex"},
    {"animal_type": "Dog", "breed": "German Shepherd", "outcome_type": "Transfer",
     "datetime": "2017-02-11 09:00:00", "name": "Bud"},
    {"animal_type": "Cat", "breed": "Domestic Shorthair Mix", "outcome_type": "Adoption",
     "datetime": "2017-03-01 12:00:00", "name": "Milo"},
    {"animal_type": "Cat", "breed": "Domestic Shorthair Mix", "outcome_type": "Adoption",
     "datetime": "2017-06-01 12:00:00", "name": "Luna"},
    {"animal_type": "Dog", "breed": "Newfoundland", "outcome_type": "Adoption",
     "datetime": "2017-07-04 08:30:00", "name": "Bear"},
]


@pytest.fixture()
def shelter():
    client = mongomock.MongoClient()
    shelter = AnimalShelter(client=client, db="AAC_TEST", collection="animals_test")
    shelter.collection.insert_many([dict(doc) for doc in SAMPLE])
    return shelter


def test_read_all_suppresses_object_id(shelter):
    rows = shelter.read({})
    assert len(rows) == len(SAMPLE)
    assert all("_id" not in row for row in rows)


def test_read_none_means_match_everything(shelter):
    assert len(shelter.read(None)) == len(SAMPLE)


def test_read_with_filter(shelter):
    dogs = shelter.read({"animal_type": "Dog"})
    assert len(dogs) == 3
    assert {d["name"] for d in dogs} == {"Rex", "Bud", "Bear"}


def test_create_inserts_document(shelter):
    assert shelter.create({"animal_type": "Bird", "name": "Kiwi"}) is True
    assert len(shelter.read({"animal_type": "Bird"})) == 1


def test_create_rejects_non_dict(shelter):
    with pytest.raises(ValidationError):
        shelter.create(["not", "a", "dict"])


def test_update_sets_values(shelter):
    modified = shelter.update({"name": "Rex"}, {"outcome_type": "Return to Owner"})
    assert modified == 1
    assert shelter.read({"name": "Rex"})[0]["outcome_type"] == "Return to Owner"


def test_delete_requires_non_empty_query(shelter):
    with pytest.raises(ValidationError):
        shelter.delete({})


def test_delete_removes_matches(shelter):
    deleted = shelter.delete({"animal_type": "Cat"})
    assert deleted == 2
    assert len(shelter.read({"animal_type": "Cat"})) == 0


@pytest.mark.parametrize("bad_query", [
    {"$where": "this.name == 'Rex'"},
    {"breed": {"$expr": {"$eq": [1, 1]}}},
    {"nested": {"deep": {"$function": {"body": "x", "args": [], "lang": "js"}}}},
])
def test_read_rejects_injection_operators(shelter, bad_query):
    with pytest.raises(ValidationError):
        shelter.read(bad_query)


def test_ensure_indexes_creates_expected(shelter):
    shelter.ensure_indexes()
    names = shelter.list_indexes()
    assert "idx_animal_type" in names
    assert "idx_breed" in names
    assert "idx_outcome_datetime" in names


def test_ensure_indexes_is_idempotent(shelter):
    first = shelter.ensure_indexes()
    second = shelter.ensure_indexes()
    assert first == second


def test_pagination_bounds_and_metadata(shelter):
    page1 = shelter.read_page({}, page=1, page_size=2, sort=[("name", 1)])
    assert page1["page_size"] == 2
    assert page1["total"] == len(SAMPLE)
    assert page1["total_pages"] == 3
    assert len(page1["records"]) == 2
    page3 = shelter.read_page({}, page=3, page_size=2, sort=[("name", 1)])
    assert len(page3["records"]) == 1  # remainder


def test_pagination_rejects_bad_page(shelter):
    with pytest.raises(ValidationError):
        shelter.read_page({}, page=0)


def test_aggregation_outcomes_by_type(shelter):
    results = shelter.outcomes_by_type()
    # Cat/Adoption should be 2; Dog/Adoption should be 2; each other pair 1.
    lookup = {(r["animal_type"], r["outcome_type"]): r["count"] for r in results}
    assert lookup[("Cat", "Adoption")] == 2
    assert lookup[("Dog", "Adoption")] == 2
    assert lookup[("Dog", "Transfer")] == 1


def test_aggregation_date_range_filters(shelter):
    # Only Jan-Mar 2017: Rex (Jan), Bud (Feb), Milo (Mar). Luna (Jun) & Bear (Jul) excluded.
    results = shelter.outcomes_by_type(start_date="2017-01-01", end_date="2017-03-31")
    total = sum(r["count"] for r in results)
    assert total == 3


def test_aggregate_rejects_disallowed_stage(shelter):
    with pytest.raises(ValidationError):
        shelter.aggregate([{"$out": "somewhere"}])
