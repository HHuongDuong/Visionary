<template>
    <Camera :resolution="{ width: 375, height: 812 }" ref="camera" autoplay></Camera>
    <Buttonbar :defaultSelected="selectedButtonName" @update:selectedButton="updateSelectedButton" />
    <!-- <img v-if="snapshotUrl" :src="snapshotUrl" alt="Snapshot" /> -->
</template>

<script setup lang="ts">
import Camera from 'simple-vue-camera';

type ButtonName = 'Text' | 'Face' | 'Object' | 'Product' | 'Distance';

const camera = ref<InstanceType<typeof Camera>>();
const snapshotUrl = ref<string | null>(null);
const selectedButtonName = ref<ButtonName>('Text');
const jsonResponse = ref<object | null>(null);

const endpoints: Record<ButtonName, string> = {
    'Text': '/document_recognition',
    'Face': '/face_detection/recognize',
    'Object': '/image_captioning',
    'Product': '/product_recognition',
    'Distance': '/distance_estimate'
};

const snapshot = async () => {
    const blob = await camera.value?.snapshot();
    if (blob) {
        snapshotUrl.value = URL.createObjectURL(blob);
        // console.log('Snapshot URL:', snapshotUrl.value);
        sendImageToEndpoint(blob);
    }
};

const sendImageToEndpoint = async (blob: Blob) => {
    const endpoint = endpoints[selectedButtonName.value];
    if (!endpoint) return;

    const formData = new FormData();
    formData.append('file', blob); // Ensure 'file' matches the expected key on the backend

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            body: formData,
            headers: {
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        jsonResponse.value = await response.json();
        console.log('JSON Response:', jsonResponse.value);
    } catch (error) {
        console.error('Error sending image to endpoint:', error);
    }
};

const updateSelectedButton = (buttonName: ButtonName) => {
    selectedButtonName.value = buttonName;
};

let intervalId: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
    intervalId = setInterval(snapshot, 5000); 
});

onUnmounted(() => {
    if (intervalId) {
        clearInterval(intervalId); 
    }
});
</script>

<style scoped></style>