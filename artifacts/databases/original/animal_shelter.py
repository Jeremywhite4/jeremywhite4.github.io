from pymongo import MongoClient
from bson.objectid import ObjectId


class AnimalShelter(object):
    """CRUD operations for Animal collection in MongoDB"""

    def __init__(self, username=None, password=None):
        # Connection Variables
        USER = username
        PASS = password
        HOST = 'nv-desktop-services.apporto.com'
        PORT = 30296
        DB = 'AAC'
        COL = 'animals'

        # Initialize Connection
        self.client = MongoClient('mongodb://%s:%s@%s:%d/?authSource=AAC' % (USER, PASS, HOST, PORT))
        self.database = self.client['%s' % (DB)]
        self.collection = self.database['%s' % (COL)]

    def create(self, data):
        if data is not None:
            result = self.collection.insert_one(data)
            return result.acknowledged
        else:
            raise Exception("Nothing to save, because data parameter is empty")

    def read(self, query):
        if query is not None:
            cursor = self.collection.find(query, {"_id": False})
            return list(cursor)
        else:
            raise Exception("Nothing to read, because data parameter is empty")

    def update(self, query, new_values):
        if query is not None and new_values is not None:
            result = self.collection.update_many(query, {"$set": new_values})
            return result.modified_count
        else:
            raise Exception("Nothing to update, because query or new_values parameter is empty")

    def delete(self, query):
        if query is not None:
            result = self.collection.delete_many(query)
            return result.deleted_count
        else:
            raise Exception("Nothing to delete, because query parameter is empty")
