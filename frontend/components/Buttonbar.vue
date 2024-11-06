<template>
    <div class="fixed bottom-0 left-0 w-full overflow-x-auto no-scrollbar p-8">
        <div class="flex space-x-8 after:content-[''] after:flex-shrink-0 after:w-8">
            <button v-for="(button, index) in buttons" :key="index"
                :class="{ 'btn btn-outline btn-primary h-20 w-20 flex flex-col items-center justify-center': 
                selectedButton !== index, 'btn btn-primary h-20 w-20 flex flex-col items-center justify-center': selectedButton === index }"
                @click="selectButton(index)">
                <i :class="icons[index]"></i>
                <span>{{ button }}</span>
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">

const props = defineProps({
    defaultSelected: {
        type: String,
        default: 'Text'
    }
});

const emits = defineEmits(['update:selectedButton']);

const selectedButton = ref<number | null>(null);
const buttons = ['Text', 'Face', 'Object', 'Product', 'Distance'];
const icons = ['fa-solid fa-quote-right', 'fa-solid fa-smile', 'fa-solid fa-cube', 'fa-solid fa-shopping-cart', 'fa-solid fa-ruler'];

const selectButton = (index: number) => {
    selectedButton.value = index;
    emits('update:selectedButton', buttons[index]);
};

selectedButton.value = buttons.indexOf(props.defaultSelected);
</script>