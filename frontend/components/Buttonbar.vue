<template>
    <div class="w-full overflow-x-auto no-scrollbar">
            <div class="flex py-4">
                <button v-for="(button, index) in buttons" :key="index"
                    :class="{ 'btn btn-lg btn-ghost flex flex-col items-center justify-center': 
                    selectedButton !== index, 'btn btn-lg btn-ghost text-primary flex flex-col items-center justify-center': selectedButton === index }"
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