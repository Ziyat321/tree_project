package kz.runtime.tree;

import jakarta.persistence.*;
import kz.runtime.tree.entity.Subcategory;

import java.util.List;

public class TreeMain {


    static EntityManagerFactory factory = Persistence.createEntityManagerFactory("main");

    public static void main(String[] args) {

        //EntityManager manager = factory.createEntityManager();

        //getHierarchy();
        //addSubcategory(2, "Processor1");
        //getSubhierarchy("Процессоры");
        //deleteSubcategory(2);
        //getSubhierarchy("Процессоры");
        moveSubcategory(2, 0);
    }

    static void getHierarchy() {
        EntityManager manager = factory.createEntityManager();
        TypedQuery<Subcategory> subcategoryQuery = manager.createQuery(
                "select s from Subcategory s order by s.left_key", Subcategory.class
        );
        List<Subcategory> subcategories = subcategoryQuery.getResultList();
        for (Subcategory subcategory : subcategories) {
            int level = subcategory.getLevel().intValue();
            System.out.println("- ".repeat(level) + subcategory.getName());
        }
    }

    static void getSubhierarchy(String startingPoint) {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("main");
        EntityManager manager = factory.createEntityManager();
        TypedQuery<Subcategory> subcategoryQuery = manager.createQuery(
                "select s from Subcategory s where s.name = ?1", Subcategory.class
        );
        subcategoryQuery.setParameter(1, startingPoint);
        Subcategory subcategory = subcategoryQuery.getSingleResult();
        int levelSubtract = subcategory.getLevel().intValue();
        int left_key = subcategory.getLeft_key().intValue();
        int right_key = subcategory.getRight_key().intValue();
        TypedQuery<Subcategory> subcategoryQuery1 = manager.createQuery(
                "select s from Subcategory s where s.left_key between ?1 and ?2 order by s.left_key", Subcategory.class
        );
        subcategoryQuery1.setParameter(1, left_key);
        subcategoryQuery1.setParameter(2, right_key);
        List<Subcategory> subcategories = subcategoryQuery1.getResultList();
        for (Subcategory subcategory1 : subcategories) {
            int level = subcategory1.getLevel().intValue() - levelSubtract;
            System.out.println("- ".repeat(level) + subcategory1.getName());
        }
    }

    static void addSubcategory(int id, String name) {
        EntityManager manager = factory.createEntityManager();
        try {
            if (id == 0) {
                manager.getTransaction().begin();
                TypedQuery<Long> subcategoryQuery = manager.createQuery(
                        "select max(s.right_key) from Subcategory s", Long.class
                );
                Long rightKeyMax = subcategoryQuery.getSingleResult();
                long leftKeyNew = rightKeyMax + 1;
                long rightKeyNew = leftKeyNew + 1;
                long levelNew = 0;
                Subcategory subcategoryNew = new Subcategory();
                subcategoryNew.setName(name);
                subcategoryNew.setLeft_key(leftKeyNew);
                subcategoryNew.setRight_key(rightKeyNew);
                subcategoryNew.setLevel(levelNew);
                manager.persist(subcategoryNew);
                manager.getTransaction().commit();
            } else {
                manager.getTransaction().begin();
                TypedQuery<Subcategory> subcategoryQuery = manager.createQuery(
                        "select s from Subcategory s where s.id = ?1", Subcategory.class
                );
                subcategoryQuery.setParameter(1, id);
                Subcategory subcategoryMain = subcategoryQuery.getSingleResult();
                long right_keyMain = subcategoryMain.getRight_key();
                long levelMain = subcategoryMain.getLevel();


                Query updateQuery = manager.createQuery(
                        """
                                         update Subcategory s 
                                         set s.left_key = s.left_key + 2 where s.right_key > ?1 
                                         and s.left_key > ?1
                                         
                                         
                                """
                );
                updateQuery.setParameter(1, right_keyMain);
                updateQuery.executeUpdate();
                Query updateQuery1 = manager.createQuery(
                        """
                                update Subcategory s 
                                set s.right_key = s.right_key + 2 
                                where s.right_key > ?1 
                                """
                );
                updateQuery1.setParameter(1, right_keyMain);
                updateQuery1.executeUpdate();

                subcategoryMain.setRight_key(right_keyMain + 2);
                Subcategory subcategoryNew = new Subcategory();
                subcategoryNew.setLeft_key(right_keyMain);
                subcategoryNew.setRight_key(right_keyMain + 1);
                subcategoryNew.setName(name);
                subcategoryNew.setLevel(levelMain + 1);
                manager.persist(subcategoryMain);
                manager.persist(subcategoryNew);
                manager.getTransaction().commit();
            }
        } catch (Exception e) {
            manager.getTransaction().rollback();
            System.out.println(e.getMessage());
        }
    }

    static void deleteSubcategory(int id) {
        EntityManager manager = factory.createEntityManager();
        try {
            manager.getTransaction().begin();
            Subcategory categoryDeleted = manager.find(Subcategory.class, id);
            long leftKey = categoryDeleted.getLeft_key();
            long rightKey = categoryDeleted.getRight_key();


            long subtractor = rightKey - leftKey + 1;

            Query deleteQuery = manager.createQuery(
                    """
                            delete from Subcategory s 
                            where s.left_key between ?1 and ?2
                            """
            );
            deleteQuery.setParameter(1, leftKey);
            deleteQuery.setParameter(2, rightKey);
            deleteQuery.executeUpdate();


            Query updateQuery = manager.createQuery(
                    """
                            update Subcategory s 
                            set s.right_key = s.right_key - ?1 
                            where s.left_key < ?2
                            and s.right_key > ?3
                            """
            );
            updateQuery.setParameter(1, subtractor);
            updateQuery.setParameter(2, leftKey);
            updateQuery.setParameter(3, rightKey);
            updateQuery.executeUpdate();

            Query updateQuery1 = manager.createQuery(
                    """
                              update Subcategory s 
                              set s.left_key = s.left_key - ?1,
                              s.right_key = s.right_key - ?1 
                              where s.left_key > ?2
                            """
            );
            updateQuery1.setParameter(1, subtractor);
            updateQuery1.setParameter(2, rightKey);
            updateQuery1.executeUpdate();
            manager.getTransaction().commit();
        } catch (Exception e) {
            manager.getTransaction().rollback();
        }
    }

    static void moveSubcategory(int fromId, int toId) {
        EntityManager manager = factory.createEntityManager();
        Subcategory to = manager.find(Subcategory.class, toId);
        Subcategory from = manager.find(Subcategory.class, fromId);
        if (to == null || from.getRight_key() < to.getLeft_key() ||
                from.getLevel() >= to.getLevel()) {
            manager.clear();
            try {
                manager.getTransaction().begin();

                Subcategory fromCategory = manager.find(Subcategory.class, fromId);
                long fromLeftKey = fromCategory.getLeft_key();
                long fromRightKey = fromCategory.getRight_key();
                long size = fromRightKey - fromLeftKey + 1;


                // обновить ключи нужной кучки
                Query updateQuery = manager.createQuery(
                        """
                                update Subcategory t
                                set t.left_key = -1 * t.left_key,
                                t.right_key = -1 * t.right_key
                                where t.left_key >= ?1
                                and t.right_key <= ?2
                                """
                );
                updateQuery.setParameter(1, fromLeftKey);
                updateQuery.setParameter(2, fromRightKey);
                updateQuery.executeUpdate();

                // поменять ключи родителей
                Query parentCategoryQuery = manager.createQuery(
                        """
                                update Subcategory t
                                set t.right_key = t.right_key - ?1 
                                where t.left_key < ?2
                                and t.right_key > ?3
                                 """
                );
                parentCategoryQuery.setParameter(1, size);
                parentCategoryQuery.setParameter(2, fromLeftKey);
                parentCategoryQuery.setParameter(3, fromRightKey);
                parentCategoryQuery.executeUpdate();

                // поменять ключи всех нижних
                Query nextCategoriesQuery = manager.createQuery(
                        """
                                  update Subcategory t 
                                  set t.left_key = t.left_key - ?1,
                                  t.right_key = t.right_key - ?1 
                                  where t.left_key > ?2
                                """
                );
                nextCategoriesQuery.setParameter(1, size);
                nextCategoriesQuery.setParameter(2, fromRightKey);
                nextCategoriesQuery.executeUpdate();





                if (toId == 0) {
                    TypedQuery<Long> subcategoryQuery = manager.createQuery(
                            "select max(s.right_key) from Subcategory s", Long.class
                    );
                    Long rightKeyMax = subcategoryQuery.getSingleResult();
                    long summand = rightKeyMax + 1 - fromLeftKey;
                    long levelDecreaser = fromCategory.getLevel();
                    Query updateQueryPack = manager.createQuery(
                            """
                                    update Subcategory s 
                                    set s.left_key = abs(s.left_key) + ?1,
                                    s.right_key = abs(s.right_key) + ?1,
                                    s.level = s.level - ?2 
                                    where s.left_key < 0
                                    """
                    );
                    updateQueryPack.setParameter(1, summand);
                    updateQueryPack.setParameter(2, levelDecreaser);
                    updateQueryPack.executeUpdate();
                } else {
                    Subcategory toCategory = manager.find(Subcategory.class, toId);
                    long toLeftKey = toCategory.getLeft_key();
                    long toRightKey = toCategory.getRight_key();

                    // обновить ключи нижних подкатегорий с учётом переноса
                    Query subCategoriesQuery = manager.createQuery(
                            """
                                    update Subcategory t
                                    set t.left_key = t.left_key + ?1,
                                    t.right_key = t.right_key + ?1
                                    where t.left_key > ?2
                                    """
                    );
                    subCategoriesQuery.setParameter(1, size);
                    subCategoriesQuery.setParameter(2, toRightKey);
                    subCategoriesQuery.executeUpdate();

                    //обновить ключи пачки переносимой
                    long summand = toRightKey - fromLeftKey;
                    long levelSummand = toCategory.getLevel() + 1 - fromCategory.getLevel();
                    Query updateQueryPack = manager.createQuery(
                            """
                                    update Subcategory t 
                                    set t.left_key = abs(t.left_key) + ?1,
                                    t.right_key = abs(t.right_key) + ?1,
                                    t.level = t.level + ?2
                                    where t.left_key < 0
                                    """
                    );
                    updateQueryPack.setParameter(1, summand);
                    updateQueryPack.setParameter(2, levelSummand);
                    updateQueryPack.executeUpdate();

                    // обновить правый ключ родительской категории
                    Query parentRightKeyUpdate = manager.createQuery(
                            """
                                    update Subcategory t
                                    set t.right_key = t.right_key + ?1
                                    where t.left_key <= ?2 
                                    and t.right_key >= ?3
                                    """
                    );
                    parentRightKeyUpdate.setParameter(1, size); // аудит - 21
                    parentRightKeyUpdate.setParameter(2, toLeftKey);
                    parentRightKeyUpdate.setParameter(3, toRightKey);
                    parentRightKeyUpdate.executeUpdate();
                }



                manager.getTransaction().commit();
            } catch (Exception e) {
                manager.getTransaction().rollback();
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Невозможно перенсти категорию в свою подкатегорию.");
        }
    }
}
