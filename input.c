
typedef struct {
    int *data;
    size_t size;
    size_t capacity;
} Vector;

void vector_init(Vector *vec) {
    vec->data = NULL;
    vec->size = 0;
    vec->capacity = 0;
}

void vector_append(Vector *vec, int value) {
    if (vec->size == vec->capacity) {
        vec->capacity = vec->capacity == 0 ? 1 : vec->capacity * 2;
        vec->data = (int *) realloc(vec->data, vec->capacity * sizeof(vec->data[0]));
    }
    vec->data[vec->size++] = value;
}

void vector_remove(Vector *vec, size_t index) {
    if (index >= vec->size) {
        fprintf(stderr, "Index out of bounds\n");
        exit(1);
    }
    for (int i = index; i < vec->size - 1; i++) {
        vec->data[i] = vec->data[i + 1];
    }
    vec->size--;
}

int vector_get( Vector *vec, size_t index) {
    if (index >= vec->size) {
        fprintf(stderr, "Index out of bounds\n");
        exit(1);
    }
    return vec->data[index];
}

void vector_set(Vector *vec, size_t index, int value) {
    if (index >= vec->size) {
        fprintf(stderr, "Index out of bounds\n");
        exit(1);
    }
    vec->data[index] = value;
}

void vector_add(Vector *vec1, Vector *vec2) {
    for (int i = 0; i < vec2->size; i++) {
        vector_append(vec1, vector_get(vec2, i));
    }
}
void vector_subtract(Vector *vec1, Vector *vec2) {
    for (int i = 0; i < vec2->size; i++) {
        for (int j = 0; j < vec1->size; j++) {
            if (vector_get(vec1, j) == vector_get(vec2, i)) {
                vector_remove(vec1, j);
                break;
            }
        }
    }
}
void vector_free(Vector *vec) {
    free(vec->data);
    vec->data = NULL;
    vec->size = 0;
    vec->capacity = 0;
}
int vector_multiply( Vector *vec1, Vector *vec2) {
    if (vec1->size != vec2->size) {
        fprintf(stderr, "Vectors must have the same size\n");
        exit(1);
    }
    int result = 0;
    for (int i = 0; i < vec1->size; i++) {
        result += vector_get(vec1, i) * vector_get(vec2, i);
    }
    return result;
}
int vector_sum(Vector *vec) {
    int sum = 0;
    for (size_t i = 0; i < vec->size; i++) {
        sum += vector_get(vec, i);
    }
    return sum;
}

int main() {
    Vector vec1;
    Vector vec2;
    vector_init(&vec1);
    vector_init(&vec2);

    vector_append(&vec1, 10);
    vector_append(&vec1, 20);
    vector_append(&vec1, 30);

    vector_append(&vec2, 40);
    vector_append(&vec2, 50);
    vector_append(&vec2, 60);
    printf("Vector 1: ");
    for (int i = 0; i < vec1.size; i++) {
        printf("%d ", vector_get(&vec1, i));
    }
    printf("\n");
    printf("Vector 2: ");
        for (int i = 0; i < vec2.size; i++) {
            printf("%d ", vector_get(&vec2, i));
        }
        printf("\n");
    vector_add(&vec1, &vec2);
    printf("Vector 1 after adding vector 2: ");
    for (int i = 0; i < vec1.size; i++) {
        printf("%d ", vector_get(&vec1, i));
    }
    printf("\n");
    vector_subtract(&vec1, &vec2);
    printf("Vector 1 after removing vector 2: ");
        for (int i = 0; i < vec1.size; i++) {
            printf("%d ", vector_get(&vec1, i));
        }
    printf("\n");

    int multiply = vector_multiply(&vec1, &vec2);
    printf("Multiply of vectors: %d", multiply);
    printf("\n");
    int sum1 = vector_sum(&vec1);
    printf("Sum of vector 1: %d", sum1);
    printf("\n");
    int sum2 = vector_sum(&vec2);
    printf("Sum of vector 2: %d", sum2);
    printf("\n");

    printf("Second element of vector 1: %d", vector_get(&vec1, 1));
    printf("\n");
    printf("Third element of vector 2: %d", vector_get(&vec2, 2));
    printf("\n");
    return 0;
}