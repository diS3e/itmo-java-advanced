package info.kgeorgiy.ja.samodelov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {

    private final Comparator<? super Student> ID_COMPARATOR = Comparator.comparingInt(Student::getId);

    private final Comparator<? super Student> NAME_ID_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .reversed()
                    .thenComparing(Student::getId);

    private <T> List<T> mapStudentsList(List<Student> students, Function<Student, T> function) {
        return students.stream().map(function).toList();
    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudentsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudentsList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapStudentsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudentsList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(getFirstNames(students));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(ID_COMPARATOR)
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortByComparator(Collection<Student> students, Comparator<? super Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .toList();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortByComparator(students, ID_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortByComparator(students, NAME_ID_COMPARATOR);

    }

    private <T> List<Student> findStudentsByAttribute(Collection<Student> students, T value, Function<Student, T> f) {
        return students.stream()
                .filter(it -> f.apply(it).equals(value))
                .sorted(NAME_ID_COMPARATOR)
                .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByAttribute(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByAttribute(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByAttribute(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)
                ));
    }

    private List<Group> getGroupsByFunction(Collection<Student> students, Function<Collection<Student>, List<Student>> function) {
        return students.stream()
                .sorted(Comparator.comparing(Student::getGroup))
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .map(it -> new Group(it.getKey(), function.apply(it.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    // :NOTE: Дублирование кода с getGroupsById
    //fixed
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsByName);
    }

    // :NOTE: Дублирование кода с getGroupsByName
    //fixed
    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsById);
    }

    // :NOTE: Давайте обойдемся без тернарного оператора
    //fixed
    private <K, T> K getValueFromEntryOrNull(AbstractMap.Entry<K, T> entry, K defaultValue) {
        return Optional.ofNullable(entry).map(Map.Entry::getKey).orElse(defaultValue);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getValueFromEntryOrNull(
                students.stream()
                        .sorted(Comparator.comparing(Student::getGroup))
                        .collect(Collectors.groupingBy(Student::getGroup))
                        .entrySet()
                        .stream()
                        .max(Comparator.comparingInt(
                                (Map.Entry<GroupName, List<Student>> it) ->
                                        it.getValue().size()).thenComparing(Map.Entry::getKey)
                        )
                        .orElse(null), null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getValueFromEntryOrNull(
                getGroupsByName(students)
                        .stream()
                        .map(it -> new AbstractMap.SimpleEntry<>(it.getName(),
                                getDistinctFirstNames(it.getStudents()).size()))
                        .max(Comparator.comparingInt(Map.Entry::getValue))
                        .orElse(null), null);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getValueFromEntryOrNull(students.stream()
                .collect(Collectors.groupingBy(Student::getFirstName,
                        Collectors.mapping(Student::getGroup, Collectors.toSet())))
                // :NOTE: ооооооооооочень длинная строчка
                //fixed
                .entrySet().stream()
                .max(Comparator
                        .comparingInt((AbstractMap.Entry<String, Set<GroupName>> it) -> it.getValue().size())
                        .thenComparing(Comparator.comparing(AbstractMap.Entry<String, Set<GroupName>>::getKey)
                        .reversed()))
                .orElse(null), "");
    }

    private <T> List<T> getWithIDsFromMap(Map<Integer, Student> students, final int[] ids, Function<Student, T> f) {
        return Arrays.stream(ids).mapToObj(it -> f.apply(students.get(it))).toList();
    }


    private <T> List<T> getWithIDsFromArray(Collection<Student> students, final int[] ids, Function<Student, T> f) {
        return getWithIDsFromMap(students.stream().collect(
                Collectors.toMap(Student::getId, Function.identity())), ids, f);
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] ids) {
        return getWithIDsFromArray(students, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] ids) {
        return getWithIDsFromArray(students, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] ids) {
        return getWithIDsFromArray(students, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] ids) {
        return getWithIDsFromArray(students, ids, this::getFullName);
    }
}
